using System.Globalization;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Utils;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class DietitianService(
    IClientRepository clientRepository,
    IMealLogRepository mealLogRepository,
    IDietProgramRepository dietProgramRepository,
    IDietProgramHistoryRepository dietProgramHistoryRepository,
    IDietitianRepository dietitianRepository,
    IKitchenChefRecipeLogRepository kitchenChefRecipeLogRepository,
    ICriticalAlertService criticalAlertService,
    IClientPdfAnalysisRepository clientPdfAnalysisRepository,
    IActivityLogService activityLogService) : IDietitianService
{
    public async Task<object> GetClientsWithLastMealAsync(string dietitianId)
    {
        var clients = await clientRepository.GetByDietitianIdAsync(dietitianId);
        var response = new List<object>();

        foreach (var client in clients)
        {
            var lastMeal = await mealLogRepository.GetLastByClientIdAsync(client.Id!);
            response.Add(new
            {
                client.Id,
                client.FirstName,
                client.LastName,
                client.TargetCalories,
                LastMeal = lastMeal
            });
        }

        return response;
    }

    public async Task<IReadOnlyList<string>> GetDietProgramDatesAsync(string dietitianId, string clientId)
    {
        if (string.IsNullOrWhiteSpace(clientId)) throw new AppException("Danışan seçin.");
        var client = await clientRepository.GetByIdAsync(clientId);
        if (client is null) throw new AppException("Danışan bulunamadı.");
        if (client.DietitianId != dietitianId)
        {
            throw new AppException("Sadece kendi danışanlarınızın programlarını görebilirsiniz.");
        }

        return await dietProgramRepository.GetProgramDatesByDietitianAndClientAsync(dietitianId, clientId);
    }

    public async Task<DietProgramViewDto> GetDietProgramAsync(string dietitianId, string clientId, string programDate)
    {
        if (string.IsNullOrWhiteSpace(clientId)) throw new AppException("Danışan seçin.");
        if (string.IsNullOrWhiteSpace(programDate)) throw new AppException("Tarih seçin.");
        var client = await clientRepository.GetByIdAsync(clientId);
        if (client is null) throw new AppException("Danışan bulunamadı.");
        if (client.DietitianId != dietitianId)
        {
            throw new AppException("Sadece kendi danışanlarınızın programını görebilirsiniz.");
        }

        var dateKey = ProgramDateHelper.TryNormalize(programDate);
        if (dateKey is null) throw new AppException("Gecerli tarih formati: yyyy-MM-dd.");
        var p = await dietProgramRepository.GetByDietitianClientAndProgramDateAsync(dietitianId, clientId, dateKey);
        if (p is null)
        {
            return new DietProgramViewDto
            {
                ClientId = clientId,
                ProgramDate = dateKey,
                HasSavedProgram = false
            };
        }

        return new DietProgramViewDto
        {
            ClientId = p.ClientId,
            ProgramDate = p.ProgramDate,
            Breakfast = p.Breakfast,
            Lunch = p.Lunch,
            Dinner = p.Dinner,
            Snack = p.Snack,
            BreakfastCalories = p.BreakfastCalories,
            LunchCalories = p.LunchCalories,
            DinnerCalories = p.DinnerCalories,
            SnackCalories = p.SnackCalories,
            TotalCalories = p.TotalCalories,
            HasSavedProgram = true,
            UpdatedAt = p.UpdatedAt,
            BreakfastCompleted = p.BreakfastCompleted,
            LunchCompleted = p.LunchCompleted,
            DinnerCompleted = p.DinnerCompleted,
            SnackCompleted = p.SnackCompleted
        };
    }

    public async Task SaveDietProgramAsync(string dietitianId, SaveDietProgramDto dto)
    {
        var client = await clientRepository.GetByIdAsync(dto.ClientId);
        if (client is null) throw new AppException("Danisan bulunamadi.");
        if (client.DietitianId != dietitianId)
        {
            throw new AppException("Sadece kendi danisanlariniza program atayabilirsiniz.");
        }

        var dateKey = ProgramDateHelper.TryNormalize(dto.ProgramDate);
        if (dateKey is null)
        {
            throw new AppException("Gecerli tarih zorunludur (yyyy-MM-dd).");
        }

        var existing = await dietProgramRepository.GetByDietitianClientAndProgramDateAsync(dietitianId, dto.ClientId, dateKey);
        if (existing is null && ProgramDateHelper.IsBeforeTodayUtc(dateKey))
        {
            throw new AppException("Gecmis tarihe yeni program atanamaz; bugun veya ileri bir tarih secin.");
        }

        if (existing is not null)
        {
            await dietProgramHistoryRepository.ArchiveCurrentBeforeUpdateAsync(
                existing,
                DateTime.UtcNow);
        }

        var model = existing ?? new DietProgram
        {
            ClientId = dto.ClientId,
            ProgramDate = dateKey,
            DietitianId = dietitianId
        };

        static int N(int v) => v < 0 ? 0 : v;
        var bc = N(dto.BreakfastCalories);
        var lc = N(dto.LunchCalories);
        var dc = N(dto.DinnerCalories);
        var sc = N(dto.SnackCalories);
        var sum = bc + lc + dc + sc;

        model.Breakfast = dto.Breakfast;
        model.Lunch = dto.Lunch;
        model.Dinner = dto.Dinner;
        model.Snack = dto.Snack;
        model.BreakfastCalories = bc;
        model.LunchCalories = lc;
        model.DinnerCalories = dc;
        model.SnackCalories = sc;
        model.TotalCalories = sum;
        model.UpdatedAt = DateTime.UtcNow;

        await dietProgramRepository.UpsertAsync(model);

        var d = await dietitianRepository.GetByIdAsync(dietitianId);
        if (d is not null)
        {
            var name = $"Dr. {d.FirstName} {d.LastName}".Trim();
            await activityLogService.LogAsync(dietitianId, name, "Diyet programı güncelledi");
        }
    }

    public async Task<IReadOnlyList<KitchenChefRecipeLogItemDto>> GetClientKitchenRecipeLogsAsync(
        string dietitianId,
        string clientId,
        int take,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(clientId)) throw new AppException("Danisan secin.");
        var client = await clientRepository.GetByIdAsync(clientId);
        if (client is null) throw new AppException("Danisan bulunamadi.");
        if (client.DietitianId != dietitianId)
            throw new AppException("Sadece kendi danisaninizin AI Mutfak kayitlarini gorebilirsiniz.");

        var rows = await kitchenChefRecipeLogRepository.GetByClientIdAsync(clientId, take, cancellationToken);
        return rows.Select(KitchenChefRecipeLogMapping.ToItemDto).ToList();
    }

    public async Task<ClientBriefDto?> GetClientBriefAsync(string dietitianId, string clientId, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(clientId)) throw new AppException("Danisan secin.");
        var client = await clientRepository.GetByIdAsync(clientId);
        if (client is null) return null;
        if (client.DietitianId != dietitianId)
            throw new AppException("Sadece kendi danisaninizin bilgilerini gorebilirsiniz.");
        return new ClientBriefDto
        {
            ClientId = client.Id!,
            FirstName = client.FirstName,
            LastName = client.LastName,
            Email = client.Email,
            TargetCalories = client.TargetCalories,
            Weight = client.Weight,
            Height = client.Height,
            Phone = string.IsNullOrWhiteSpace(client.Phone) ? string.Empty : client.Phone.Trim()
        };
    }

    public async Task<DietitianMyClientsResponseDto> GetMyClientsAsync(
        string dietitianId,
        string sort,
        string tab,
        CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(dietitianId))
            return new DietitianMyClientsResponseDto();

        var ascending = !string.Equals(sort, "nameDesc", StringComparison.OrdinalIgnoreCase);
        var clients = await clientRepository.GetByDietitianIdSortedAsync(dietitianId, ascending).ConfigureAwait(false);

        var alerts = await criticalAlertService.GetCriticalAlertsAsync(dietitianId, cancellationToken).ConfigureAwait(false);
        var criticalIds = new HashSet<string>(
            alerts.Where(a => !string.IsNullOrWhiteSpace(a.ClientId)).Select(a => a.ClientId!),
            StringComparer.Ordinal);

        var rows = new List<DietitianClientCardDto>();
        foreach (var c in clients)
        {
            if (string.IsNullOrWhiteSpace(c.Id)) continue;

            var display = $"{c.FirstName} {c.LastName}".Trim();
            if (display.Length == 0) display = c.Email;

            var lastMeal = await mealLogRepository.GetLastByClientIdAsync(c.Id).ConfigureAwait(false);
            var pdfTop = await clientPdfAnalysisRepository.GetByClientIdAsync(c.Id, 1, cancellationToken).ConfigureAwait(false);
            var kitchenTop = await kitchenChefRecipeLogRepository.GetByClientIdAsync(c.Id, 1, cancellationToken).ConfigureAwait(false);

            DateTime? lastAct = null;
            if (lastMeal is not null)
                lastAct = lastMeal.Timestamp;
            if (pdfTop.Count > 0)
                lastAct = CombineMax(lastAct, pdfTop[0].CreatedAtUtc);
            if (kitchenTop.Count > 0)
                lastAct = CombineMax(lastAct, kitchenTop[0].CreatedAtUtc);

            var (compliance, _) = await GetComplianceSnapshotAsync(dietitianId, c.Id, cancellationToken).ConfigureAwait(false);
            var isCritical = criticalIds.Contains(c.Id);
            var segment = ClassifySegment(isCritical, lastAct);

            rows.Add(new DietitianClientCardDto
            {
                Id = c.Id,
                FirstName = c.FirstName,
                LastName = c.LastName,
                DisplayName = display,
                StartedAtUtc = c.CreatedAt,
                LastActivityUtc = lastAct,
                CompliancePercent = compliance,
                Segment = segment,
                IsCritical = isCritical
            });
        }

        var counts = new DietitianClientTabCountsDto
        {
            All = rows.Count,
            Active = rows.Count(r => string.Equals(r.Segment, "active", StringComparison.OrdinalIgnoreCase)),
            Critical = rows.Count(r => string.Equals(r.Segment, "critical", StringComparison.OrdinalIgnoreCase)),
            Passive = rows.Count(r => string.Equals(r.Segment, "passive", StringComparison.OrdinalIgnoreCase))
        };

        IEnumerable<DietitianClientCardDto> filtered = rows;
        var t = (tab ?? "all").Trim().ToLowerInvariant();
        filtered = t switch
        {
            "active" => rows.Where(r => string.Equals(r.Segment, "active", StringComparison.OrdinalIgnoreCase)),
            "critical" => rows.Where(r => string.Equals(r.Segment, "critical", StringComparison.OrdinalIgnoreCase)),
            "passive" => rows.Where(r => string.Equals(r.Segment, "passive", StringComparison.OrdinalIgnoreCase)),
            _ => rows
        };

        return new DietitianMyClientsResponseDto
        {
            TabCounts = counts,
            Clients = filtered.ToList()
        };
    }

    public async Task<DietitianClientOverviewDto?> GetClientOverviewAsync(
        string dietitianId,
        string clientId,
        CancellationToken cancellationToken = default)
    {
        var brief = await GetClientBriefAsync(dietitianId, clientId, cancellationToken).ConfigureAwait(false);
        if (brief is null) return null;

        var (compliance, refDate) = await GetComplianceSnapshotAsync(dietitianId, clientId, cancellationToken).ConfigureAwait(false);
        var weeklyDays = await GetWeeklyProgramDaysAsync(dietitianId, clientId, cancellationToken).ConfigureAwait(false);

        var kitchenRows = await kitchenChefRecipeLogRepository.GetByClientIdAsync(clientId, 40, cancellationToken).ConfigureAwait(false);
        var kitchenDtos = kitchenRows.Select(KitchenChefRecipeLogMapping.ToItemDto).ToList();

        var pdfRows = await clientPdfAnalysisRepository.GetByClientIdAsync(clientId, 40, cancellationToken).ConfigureAwait(false);
        var pdfDtos = pdfRows.Select(ToPdfListItemDto).ToList();

        return new DietitianClientOverviewDto
        {
            Client = brief,
            CompliancePercent = compliance,
            ComplianceReferenceDate = refDate,
            WeeklyProgramDays = weeklyDays,
            KitchenRecipeLogs = kitchenDtos,
            PdfAnalyses = pdfDtos
        };
    }

    private async Task<List<DietitianProgramDayOverviewDto>> GetWeeklyProgramDaysAsync(
        string dietitianId,
        string clientId,
        CancellationToken cancellationToken)
    {
        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var weekStart = StartOfIsoWeekMonday(today);
        var list = new List<DietitianProgramDayOverviewDto>(7);
        for (var i = 0; i < 7; i++)
        {
            cancellationToken.ThrowIfCancellationRequested();
            var d = weekStart.AddDays(i);
            var ymd = d.ToString(ProgramDateHelper.JsonFormat, CultureInfo.InvariantCulture);
            var weekday = TurkishWeekdayTitle(d);

            var p = await dietProgramRepository.GetCurrentByClientIdAndProgramDateAsync(clientId, ymd).ConfigureAwait(false);
            if (p is null || !string.Equals(p.DietitianId, dietitianId, StringComparison.Ordinal))
            {
                list.Add(new DietitianProgramDayOverviewDto
                {
                    ProgramDate = ymd,
                    WeekdayLabel = weekday,
                    Meals = new List<DietitianProgramMealOverviewDto>()
                });
                continue;
            }

            var meals = new List<DietitianProgramMealOverviewDto>
            {
                NewMeal("breakfast", "Kahvaltı", p.Breakfast, p.BreakfastCalories, p.BreakfastCompleted),
                NewMeal("lunch", "Öğle yemeği", p.Lunch, p.LunchCalories, p.LunchCompleted),
                NewMeal("dinner", "Akşam yemeği", p.Dinner, p.DinnerCalories, p.DinnerCompleted)
            };

            var snackRelevant =
                !string.IsNullOrWhiteSpace(p.Snack) || p.SnackCalories > 0 || p.SnackCompleted;
            if (snackRelevant)
            {
                meals.Add(NewMeal("snack", "Ara öğün", p.Snack, p.SnackCalories, p.SnackCompleted));
            }

            list.Add(new DietitianProgramDayOverviewDto
            {
                ProgramDate = ymd,
                WeekdayLabel = weekday,
                Meals = meals
            });
        }

        return list;
    }

    private static DietitianProgramMealOverviewDto NewMeal(string key, string label, string description, int calories, bool completed) =>
        new()
        {
            MealKey = key,
            Label = label,
            Description = description ?? string.Empty,
            Calories = calories < 0 ? 0 : calories,
            Completed = completed
        };

    private static DateOnly StartOfIsoWeekMonday(DateOnly date)
    {
        var diff = (7 + (int)date.DayOfWeek - (int)DayOfWeek.Monday) % 7;
        return date.AddDays(-diff);
    }

    private static string TurkishWeekdayTitle(DateOnly d)
    {
        var dt = d.ToDateTime(TimeOnly.MinValue, DateTimeKind.Utc);
        var culture = CultureInfo.GetCultureInfo("tr-TR");
        var name = culture.DateTimeFormat.GetDayName(dt.DayOfWeek);
        if (string.IsNullOrEmpty(name))
            return string.Empty;
        return char.ToUpperInvariant(name[0]) + name.Substring(1);
    }

    private static ClientPdfAnalysisListItemDto ToPdfListItemDto(ClientPdfAnalysis r) =>
        new()
        {
            Id = r.Id ?? string.Empty,
            PdfUrl = r.PdfRelativeUrl,
            OriginalFileName = r.OriginalFileName,
            DocumentType = r.DocumentType,
            Summary = r.Summary,
            KeyFindings = r.KeyFindings,
            Cautions = r.Cautions,
            SuggestedForDietitian = r.SuggestedForDietitian,
            AnalysisSource = r.AnalysisSource,
            CreatedAtUtc = r.CreatedAtUtc
        };

    private async Task<(int percent, string? refDate)> GetComplianceSnapshotAsync(
        string dietitianId,
        string clientId,
        CancellationToken cancellationToken)
    {
        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        for (var i = 0; i < 3; i++)
        {
            var d = today.AddDays(-i);
            var ymd = d.ToString(ProgramDateHelper.JsonFormat, CultureInfo.InvariantCulture);
            var p = await dietProgramRepository.GetCurrentByClientIdAndProgramDateAsync(clientId, ymd).ConfigureAwait(false);
            if (p is null || !string.Equals(p.DietitianId, dietitianId, StringComparison.Ordinal))
                continue;

            var done = (p.BreakfastCompleted ? 1 : 0) + (p.LunchCompleted ? 1 : 0) + (p.DinnerCompleted ? 1 : 0) +
                       (p.SnackCompleted ? 1 : 0);
            var pct = (int)Math.Round(100.0 * done / 4.0);
            return (pct, ymd);
        }

        return (0, null);
    }

    private static DateTime? CombineMax(DateTime? current, DateTime candidate)
    {
        if (!current.HasValue || candidate > current.Value)
            return candidate;
        return current;
    }

    private static string ClassifySegment(bool isCritical, DateTime? lastActivityUtc)
    {
        if (isCritical)
            return "critical";
        var days = lastActivityUtc.HasValue ? (DateTime.UtcNow - lastActivityUtc.Value.ToUniversalTime()).TotalDays : 999;
        return days <= 7 ? "active" : "passive";
    }
}
