using System.Globalization;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Utils;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class CriticalAlertService(
    IClientRepository clientRepository,
    IMealLogRepository mealLogRepository,
    IDietProgramRepository dietProgramRepository,
    ICriticalAlertAcknowledgmentRepository ackRepository) : ICriticalAlertService
{
    private const double CalorieOvershootRatio = 1.2;
    private const int MissedMealMinIncomplete = 2;

    public async Task<IReadOnlyList<CriticalAlertDto>> GetCriticalAlertsAsync(string dietitianId, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(dietitianId)) return Array.Empty<CriticalAlertDto>();

        var clients = await clientRepository.GetByDietitianIdAsync(dietitianId).ConfigureAwait(false);
        if (clients.Count == 0) return Array.Empty<CriticalAlertDto>();

        var clientIds = clients.Where(c => !string.IsNullOrEmpty(c.Id)).Select(c => c.Id!).ToList();
        var acks = await ackRepository.GetByDietitianIdAsync(dietitianId, cancellationToken).ConfigureAwait(false);
        var ackSet = new HashSet<string>(StringComparer.Ordinal);
        foreach (var a in acks)
        {
            if (string.IsNullOrWhiteSpace(a.ClientId) || string.IsNullOrWhiteSpace(a.AlertType) || string.IsNullOrWhiteSpace(a.ReferenceDate))
                continue;
            ackSet.Add(MakeAckKey(a.ClientId, a.AlertType, a.ReferenceDate));
        }

        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var dateStr = new[]
        {
            today.ToString(ProgramDateHelper.JsonFormat, CultureInfo.InvariantCulture),
            today.AddDays(-1).ToString(ProgramDateHelper.JsonFormat, CultureInfo.InvariantCulture),
            today.AddDays(-2).ToString(ProgramDateHelper.JsonFormat, CultureInfo.InvariantCulture)
        };

        var windowStart = UtcStartOfYmd(dateStr[2]);
        var windowEnd = UtcStartOfYmd(dateStr[0]).AddDays(1);

        var dateSet = new HashSet<string>(dateStr, StringComparer.Ordinal);

        var meals = await mealLogRepository
            .GetByClientIdsInTimestampRangeAsync(clientIds, windowStart, windowEnd, cancellationToken)
            .ConfigureAwait(false);

        var programs = await dietProgramRepository
            .GetByDietitianClientsAndProgramDatesAsync(dietitianId, clientIds, dateStr, cancellationToken)
            .ConfigureAwait(false);
        var programByClientAndDate = new Dictionary<string, DietProgram>(StringComparer.Ordinal);
        foreach (var p in programs)
        {
            if (string.IsNullOrWhiteSpace(p.ClientId) || string.IsNullOrWhiteSpace(p.ProgramDate)) continue;
            var k = p.ClientId + "\u001f" + p.ProgramDate;
            if (!programByClientAndDate.TryGetValue(k, out var existing) || p.UpdatedAt > existing.UpdatedAt)
                programByClientAndDate[k] = p;
        }

        var calByClientDay = new Dictionary<string, int>(StringComparer.Ordinal);
        foreach (var m in meals)
        {
            if (string.IsNullOrEmpty(m.ClientId)) continue;
            var ymd = ToUtcYmd(m.Timestamp);
            if (!dateSet.Contains(ymd)) continue;
            var k = m.ClientId + "\u001f" + ymd;
            calByClientDay[k] = calByClientDay.GetValueOrDefault(k) + m.Calories;
        }

        var list = new List<CriticalAlertDto>();

        foreach (var c in clients)
        {
            if (string.IsNullOrEmpty(c.Id)) continue;
            var name = $"{c.FirstName} {c.LastName}".Trim();
            if (name.Length == 0) name = c.Email;
            if (name.Length == 0) name = "Danisan";

            foreach (var ymd in dateStr)
            {
                var pKey = c.Id + "\u001f" + ymd;
                programByClientAndDate.TryGetValue(pKey, out var program);

                if (program is not null)
                {
                    var incomplete = CountIncompleteMeals(program);
                    if (incomplete >= MissedMealMinIncomplete)
                    {
                        var dto = new CriticalAlertDto
                        {
                            Id = MakeAlertId(c.Id, CriticalAlertTypes.MissedMeals, ymd),
                            ClientId = c.Id,
                            ClientName = name,
                            AlertType = CriticalAlertTypes.MissedMeals,
                            Severity = "High",
                            ReferenceDate = ymd,
                            Date = UtcNoon(ymd),
                            Message = $"{incomplete} öğün, planlanan diyet günü ({FormatTr(ymd)}) için tamamlandı olarak işaretlenmedi."
                        };
                        if (!ackSet.Contains(MakeAckKey(c.Id, CriticalAlertTypes.MissedMeals, ymd)))
                            list.Add(dto);
                    }
                }

                var tKey = c.Id + "\u001f" + ymd;
                var dayCal = calByClientDay.GetValueOrDefault(tKey);
                var targetCals = program is not null && program.TotalCalories > 0
                    ? program.TotalCalories
                    : c.TargetCalories;
                if (targetCals > 0)
                {
                    var limit = (int)Math.Ceiling(targetCals * CalorieOvershootRatio);
                    if (dayCal > limit)
                    {
                        var dto = new CriticalAlertDto
                        {
                            Id = MakeAlertId(c.Id, CriticalAlertTypes.HighCalories, ymd),
                            ClientId = c.Id,
                            ClientName = name,
                            AlertType = CriticalAlertTypes.HighCalories,
                            Severity = "High",
                            ReferenceDate = ymd,
                            Date = UtcNoon(ymd),
                            Message = $"Günlük kayıtlı kalori toplamı ~{dayCal} kcal; hedef ~{targetCals} kcal değerinin yaklaşık %20 üstünde ({FormatTr(ymd)})."
                        };
                        if (!ackSet.Contains(MakeAckKey(c.Id, CriticalAlertTypes.HighCalories, ymd)))
                            list.Add(dto);
                    }
                }
            }
        }

        return list
            .OrderByDescending(x => x.Severity == "High" ? 1 : 0)
            .ThenByDescending(x => x.Date)
            .ToList();
    }

    public async Task AcknowledgeAsync(string dietitianId, AckCriticalAlertDto dto, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(dietitianId)) throw new AppException("Oturum bulunamadi.");
        if (string.IsNullOrWhiteSpace(dto.ClientId)) throw new AppException("Danisan secin.");
        if (string.IsNullOrWhiteSpace(dto.AlertType)) throw new AppException("Uyari turu gecersiz.");
        var dateKey = ProgramDateHelper.TryNormalize(dto.ReferenceDate);
        if (dateKey is null) throw new AppException("Gecerli referans tarihi (yyyy-MM-dd) gonderin.");

        if (dto.AlertType is not (CriticalAlertTypes.MissedMeals or CriticalAlertTypes.HighCalories))
            throw new AppException("Bilinmeyen uyari turu.");

        var client = await clientRepository.GetByIdAsync(dto.ClientId).ConfigureAwait(false);
        if (client is null) throw new AppException("Danisan bulunamadi.");
        if (client.DietitianId != dietitianId)
            throw new AppException("Sadece kendi danisanlarinizin uyari kaydini kapatabilirsiniz.");

        var key = MakeAckKey(dto.ClientId, dto.AlertType, dateKey);
        var existing = await ackRepository.GetByDietitianIdAsync(dietitianId, cancellationToken).ConfigureAwait(false);
        if (existing.Any(x => x.ClientId == dto.ClientId && x.AlertType == dto.AlertType && x.ReferenceDate == dateKey))
            return;

        await ackRepository
            .AddAsync(
                new CriticalAlertAcknowledgment
                {
                    DietitianId = dietitianId,
                    ClientId = dto.ClientId,
                    AlertType = dto.AlertType,
                    ReferenceDate = dateKey
                },
                cancellationToken)
            .ConfigureAwait(false);
    }

    private static int CountIncompleteMeals(DietProgram p)
    {
        var n = 0;
        if (!p.BreakfastCompleted) n++;
        if (!p.LunchCompleted) n++;
        if (!p.DinnerCompleted) n++;
        if (!p.SnackCompleted) n++;
        return n;
    }

    private static string ToUtcYmd(DateTime ts)
    {
        var u = ts.Kind == DateTimeKind.Unspecified
            ? DateTime.SpecifyKind(ts, DateTimeKind.Utc)
            : ts.ToUniversalTime();
        return DateOnly.FromDateTime(u).ToString(ProgramDateHelper.JsonFormat, CultureInfo.InvariantCulture);
    }

    private static DateTime UtcStartOfYmd(string ymd)
    {
        if (!DateOnly.TryParseExact(ymd, ProgramDateHelper.JsonFormat, CultureInfo.InvariantCulture, DateTimeStyles.None, out var d))
            return DateTime.UtcNow.Date;
        return new DateTime(d.Year, d.Month, d.Day, 0, 0, 0, DateTimeKind.Utc);
    }

    private static DateTime UtcNoon(string ymd)
    {
        var s = UtcStartOfYmd(ymd);
        return s.AddHours(12);
    }

    private static string MakeAckKey(string clientId, string alertType, string ymd) => $"{clientId}|{alertType}|{ymd}";

    private static string MakeAlertId(string clientId, string alertType, string ymd) => $"{clientId}_{alertType}_{ymd}";

    private static string FormatTr(string ymd)
    {
        if (!DateOnly.TryParseExact(ymd, ProgramDateHelper.JsonFormat, CultureInfo.InvariantCulture, DateTimeStyles.None, out var d))
            return ymd;
        return d.ToString("d MMM yyyy", new CultureInfo("tr-TR"));
    }
}
