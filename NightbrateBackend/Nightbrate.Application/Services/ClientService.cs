using Nightbrate.Application.Exceptions;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class ClientService(
    IClientRepository clientRepository,
    IDietitianRepository dietitianRepository,
    IUserRepository userRepository,
    IMealLogRepository mealLogRepository,
    IWaterLogRepository waterLogRepository,
    IWeightLogRepository weightLogRepository,
    IActivityLogService activityLogService) : IClientService
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
        var c = await clientRepository.GetByIdAsync(clientId);
        if (c is not null)
        {
            var name = $"{c.FirstName} {c.LastName}".Trim();
            if (name.Length == 0) name = c.Email;
            var desc = string.IsNullOrWhiteSpace(dto.PhotoUrl) ? "Öğün kaydetti" : "Öğün fotoğrafı yükledi";
            await activityLogService.LogAsync(clientId, name, desc);
        }
    }

    public async Task AddWaterAsync(string clientId, AddWaterDto dto)
    {
        await waterLogRepository.AddAsync(new WaterLog { ClientId = clientId, Ml = dto.Ml });
        var c = await clientRepository.GetByIdAsync(clientId);
        if (c is not null)
        {
            var name = $"{c.FirstName} {c.LastName}".Trim();
            if (name.Length == 0) name = c.Email;
            await activityLogService.LogAsync(clientId, name, "Su tüketimi girdi");
        }
    }

    public async Task AddWeightAsync(string clientId, AddWeightDto dto)
    {
        await weightLogRepository.AddAsync(new WeightLog { ClientId = clientId, Weight = dto.Weight });
        var c = await clientRepository.GetByIdAsync(clientId);
        if (c is not null)
        {
            var name = $"{c.FirstName} {c.LastName}".Trim();
            if (name.Length == 0) name = c.Email;
            await activityLogService.LogAsync(clientId, name, "Kilo girdi");
        }
    }

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

    private static string NormalizeConnectionCodeOrThrow(string? raw)
    {
        var s = raw?.Trim() ?? string.Empty;
        if (string.IsNullOrEmpty(s)) throw new AppException("Takip kodu gerekli.");
        if (s.Length != 6) throw new AppException("Takip kodu 6 haneli olmalidir (buyuk harf ve rakam).");
        var code = s.ToUpperInvariant();
        if (!code.All(c => (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')))
            throw new AppException("Kod sadece buyuk harf ve rakam icerebilir.");
        return code;
    }

    public async Task<PreviewDietitianByCodeResultDto> PreviewDietitianByCodeAsync(ConnectToDietitianRequestDto dto)
    {
        var code = NormalizeConnectionCodeOrThrow(dto.ConnectionCode);
        var d = await dietitianRepository.GetApprovedByConnectionCodeAsync(code);
        if (d is null)
            throw new AppException("Gecersiz kod. Onayli bir diyetisyenin 6 haneli takip kodunu girin.");
        var n = $"{d.FirstName} {d.LastName}".Trim();
        if (string.IsNullOrEmpty(n)) n = d.Email;
        return new PreviewDietitianByCodeResultDto
        {
            FirstName = d.FirstName,
            LastName = d.LastName,
            DisplayName = $"Dr. {n}"
        };
    }

    public async Task<ConnectToDietitianResultDto> ConnectToDietitianAsync(
        string clientId,
        ConnectToDietitianRequestDto dto)
    {
        if (string.IsNullOrWhiteSpace(clientId)) throw new AppException("Gecerli danisan hesabi bulunamadi.");

        var code = NormalizeConnectionCodeOrThrow(dto.ConnectionCode);

        var d = await dietitianRepository.GetApprovedByConnectionCodeAsync(code);
        if (d is null)
            throw new AppException("Gecersiz kod. Onayli bir diyetisyenin 6 haneli takip kodunu girin.");
        if (string.IsNullOrEmpty(d.Id))
            throw new AppException("Diyetisyen kaydinda hata olustu.");

        var c = await clientRepository.GetByIdAsync(clientId);
        if (c is null) throw new AppException("Danisan profili bulunamadi.");

        if (!string.IsNullOrWhiteSpace(c.DietitianId))
        {
            if (c.DietitianId == d.Id)
            {
                return new ConnectToDietitianResultDto
                {
                    FirstName = d.FirstName,
                    LastName = d.LastName,
                    Message = $"Zaten Dr. {d.FirstName} {d.LastName} ile eslesmisisiniz."
                };
            }
            throw new AppException("Zaten baska bir diyetisyene baglisiniz. Mevcut eslestirme uzerine yazilamaz.");
        }

        var assigned = await clientRepository.TryAssignDietitianIfUnassignedAsync(clientId, d.Id!);
        if (!assigned)
        {
            c = await clientRepository.GetByIdAsync(clientId);
            if (c is null) throw new AppException("Danisan profili bulunamadi.");
            if (c.DietitianId == d.Id)
            {
                await userRepository.SetClientDietitianIdInUsersCollectionAsync(clientId, d.Id!);
                return new ConnectToDietitianResultDto
                {
                    FirstName = d.FirstName,
                    LastName = d.LastName,
                    Message = $"Dr. {d.FirstName} {d.LastName} ile basariyla eslestiniz."
                };
            }
            if (!string.IsNullOrWhiteSpace(c.DietitianId) && c.DietitianId != d.Id)
                throw new AppException("Zaten baska bir diyetisyene baglisiniz. Mevcut eslestirme uzerine yazilamaz.");
            throw new AppException("Eslestirme su anda gerceklesmedi. Lutfen tekrar deneyin.");
        }

        await userRepository.SetClientDietitianIdInUsersCollectionAsync(clientId, d.Id!);
        return new ConnectToDietitianResultDto
        {
            FirstName = d.FirstName,
            LastName = d.LastName,
            Message = $"Dr. {d.FirstName} {d.LastName} ile basariyla eslestiniz."
        };
    }
}
