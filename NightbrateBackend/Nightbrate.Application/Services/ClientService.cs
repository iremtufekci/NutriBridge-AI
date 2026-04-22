using Nightbrate.Application.Exceptions;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class ClientService(
    IClientRepository clientRepository,
    IDietitianRepository dietitianRepository,
    IMealLogRepository mealLogRepository,
    IWaterLogRepository waterLogRepository,
    IWeightLogRepository weightLogRepository) : IClientService
{
    public async Task LogMealAsync(string clientId, LogMealDto dto)
    {
        var meal = new MealLog
        {
            ClientId = clientId,
            PhotoUrl = dto.PhotoUrl,
            Calories = dto.Calories,
            Macros = new MacroInfo
            {
                Protein = dto.Protein,
                Carb = dto.Carb,
                Fat = dto.Fat
            }
        };
        await mealLogRepository.AddAsync(meal);
    }

    public Task AddWaterAsync(string clientId, AddWaterDto dto) =>
        waterLogRepository.AddAsync(new WaterLog { ClientId = clientId, Ml = dto.Ml });

    public Task AddWeightAsync(string clientId, AddWeightDto dto) =>
        weightLogRepository.AddAsync(new WeightLog { ClientId = clientId, Weight = dto.Weight });

    public async Task<ClientProfileDto> GetProfileAsync(string clientId)
    {
        var client = await clientRepository.GetByIdAsync(clientId);
        if (client is null) throw new AppException("Danisan profili bulunamadi.");

        string dietitianName = "Atanmadi";
        if (!string.IsNullOrWhiteSpace(client.DietitianId))
        {
            var dietitian = await dietitianRepository.GetByIdAsync(client.DietitianId);
            if (dietitian is not null)
            {
                dietitianName = $"Dr. {dietitian.FirstName} {dietitian.LastName}";
            }
        }

        return new ClientProfileDto
        {
            FirstName = client.FirstName,
            LastName = client.LastName,
            Height = client.Height,
            Weight = client.Weight,
            TargetCalories = client.TargetCalories,
            GoalText = ResolveGoal(client.TargetCalories),
            ThemePreference = client.ThemePreference,
            DietitianName = dietitianName,
            ProgramStartDate = client.CreatedAt
        };
    }

    public async Task UpdateThemePreferenceAsync(string clientId, string themePreference)
    {
        var client = await clientRepository.GetByIdAsync(clientId);
        if (client is null) throw new AppException("Danisan profili bulunamadi.");

        var normalized = themePreference.Trim().ToLowerInvariant();
        client.ThemePreference = normalized == "dark" ? "dark" : "light";
        await clientRepository.UpdateAsync(client);
    }

    private static string ResolveGoal(int targetCalories)
    {
        if (targetCalories <= 1800) return "Kilo Ver";
        if (targetCalories >= 2300) return "Kilo Al";
        return "Formu Koru";
    }
}
