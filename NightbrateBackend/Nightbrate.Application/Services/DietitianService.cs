using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class DietitianService(
    IClientRepository clientRepository,
    IMealLogRepository mealLogRepository,
    IDietProgramRepository dietProgramRepository) : IDietitianService
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

    public async Task SaveDietProgramAsync(string dietitianId, SaveDietProgramDto dto)
    {
        var client = await clientRepository.GetByIdAsync(dto.ClientId);
        if (client is null) throw new AppException("Danisan bulunamadi.");
        if (client.DietitianId != dietitianId)
        {
            throw new AppException("Sadece kendi danisanlariniza program atayabilirsiniz.");
        }

        var normalizedDay = dto.DayOfWeek.Trim();
        if (string.IsNullOrWhiteSpace(normalizedDay))
        {
            throw new AppException("Gun secimi zorunludur.");
        }

        var existing = await dietProgramRepository.GetByClientAndDayAsync(dto.ClientId, normalizedDay);
        var model = existing ?? new DietProgram
        {
            ClientId = dto.ClientId,
            DayOfWeek = normalizedDay,
            DietitianId = dietitianId
        };

        model.Breakfast = dto.Breakfast;
        model.Lunch = dto.Lunch;
        model.Dinner = dto.Dinner;
        model.Snack = dto.Snack;
        model.TotalCalories = dto.TotalCalories;
        model.UpdatedAt = DateTime.UtcNow;

        await dietProgramRepository.UpsertAsync(model);
    }
}
