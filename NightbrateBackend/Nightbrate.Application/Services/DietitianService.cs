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
}
