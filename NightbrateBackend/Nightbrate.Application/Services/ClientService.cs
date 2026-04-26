using Nightbrate.Application.Exceptions;
using Nightbrate.Application.DTOs;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Utils;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class ClientService(
    IClientRepository clientRepository,
    IDietitianRepository dietitianRepository,
    IUserRepository userRepository,
    IMealLogRepository mealLogRepository,
    IWaterLogRepository waterLogRepository,
    IWeightLogRepository weightLogRepository,
    IDietProgramRepository dietProgramRepository,
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

    public async Task UpdateProfileAsync(string clientId, UpdateClientProfileDto? dto)
    {
        if (dto is null) throw new AppException("Profil verisi gonderilmedi. Sayfayi yenileyip tekrar deneyin.");
        if (string.IsNullOrWhiteSpace(clientId)) throw new AppException("Gecerli danisan hesabi bulunamadi. Oturum suresi dolmus olabilir; tekrar giris yapin.");
        var client = await clientRepository.GetByIdAsync(clientId);
        if (client is null) throw new AppException("Danisan profili bulunamadi.");

        var fn = (dto.FirstName ?? "").Trim();
        var ln = (dto.LastName ?? "").Trim();
        if (string.IsNullOrEmpty(fn) || string.IsNullOrEmpty(ln))
            throw new AppException("Ad ve soyad zorunludur.");
        if (dto.Height is < 50 or > 250) throw new AppException("Boy 50–250 cm araliginda olmalidir.");
        if (dto.Weight is < 20 or > 400) throw new AppException("Kilo 20–400 kg araliginda olmalidir.");
        if (dto.TargetCalories is < 800 or > 6000) throw new AppException("Hedef kalori 800–6000 araliginda olmalidir.");

        client.FirstName = fn;
        client.LastName = ln;
        client.Height = dto.Height;
        client.Weight = dto.Weight;
        client.TargetCalories = dto.TargetCalories;

        await clientRepository.UpdateAsync(client);
        await userRepository.UpdateClientProfileInUsersCollectionAsync(
            clientId, fn, ln, client.Weight, client.Height, client.TargetCalories);

        var name = $"{fn} {ln}".Trim();
        if (name.Length == 0) name = client.Email;
        await activityLogService.LogAsync(clientId, name, "Kisisel profil bilgilerini guncelledi");
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
        await userRepository.UpdateThemePreferenceAllStoresAsync(clientId, themePreference);
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

    public async Task<IReadOnlyList<ClientDietProgramDayDto>> GetMyDietProgramsAsync(string clientId)
    {
        if (string.IsNullOrWhiteSpace(clientId)) throw new AppException("Gecerli danisan bulunamadi.");
        var rows = await dietProgramRepository.GetAllByClientIdAsync(clientId);
        var list = new List<ClientDietProgramDayDto>();
        foreach (var p in rows) list.Add(await ToClientDietProgramDayDtoAsync(p));

        return list.OrderBy(x => x.ProgramDate).ToList();
    }

    public async Task<ClientDietProgramDayDto?> GetMyDietProgramForDateAsync(string clientId, string programDate)
    {
        if (string.IsNullOrWhiteSpace(clientId)) throw new AppException("Gecerli danisan bulunamadi.");
        if (string.IsNullOrWhiteSpace(programDate)) throw new AppException("Program tarihi gerekli.");
        var dateKey = ProgramDateHelper.TryNormalize(programDate) ?? programDate.Trim();
        var p = await dietProgramRepository.GetCurrentByClientIdAndProgramDateAsync(clientId, dateKey);
        if (p is null) return null;
        return await ToClientDietProgramDayDtoAsync(p);
    }

    public async Task SetMyMealCompletedAsync(string clientId, string programDate, string meal)
    {
        if (string.IsNullOrWhiteSpace(clientId)) throw new AppException("Gecerli danisan bulunamadi.");
        var dateKey = ProgramDateHelper.TryNormalize(programDate);
        if (dateKey is null) throw new AppException("Gecerli program tarihi gerekli (yyyy-MM-dd).");
        if (string.IsNullOrWhiteSpace(meal)) throw new AppException("Ogun gerekli.");

        var c = await clientRepository.GetByIdAsync(clientId);
        if (c is null) throw new AppException("Danisan bulunamadi.");

        var p = await dietProgramRepository.GetCurrentByClientIdAndProgramDateAsync(clientId, dateKey);
        if (p is null) throw new AppException("Bu tarih icin program bulunamadi.");
        if (!string.IsNullOrEmpty(c.DietitianId) && p.DietitianId != c.DietitianId)
            throw new AppException("Program eslesmesi guncel degil.");

        switch (meal.Trim().ToLowerInvariant())
        {
            case "breakfast":
                p.BreakfastCompleted = true;
                break;
            case "lunch":
                p.LunchCompleted = true;
                break;
            case "dinner":
                p.DinnerCompleted = true;
                break;
            case "snack":
                p.SnackCompleted = true;
                break;
            default:
                throw new AppException("Gecersiz ogun. breakfast, lunch, dinner veya snack kullanin.");
        }

        p.UpdatedAt = DateTime.UtcNow;
        await dietProgramRepository.UpsertAsync(p);

        var name = $"{c.FirstName} {c.LastName}".Trim();
        if (name.Length == 0) name = c.Email;
        await activityLogService.LogAsync(clientId, name, $"Ogun tamamlandi: {meal} ({dateKey})");
    }

    private async Task<ClientDietProgramDayDto> ToClientDietProgramDayDtoAsync(DietProgram p)
    {
        string? dietitianName = null;
        if (!string.IsNullOrEmpty(p.DietitianId))
        {
            var d = await dietitianRepository.GetByIdAsync(p.DietitianId);
            if (d is not null)
            {
                var n = $"{d.FirstName} {d.LastName}".Trim();
                if (string.IsNullOrEmpty(n)) n = d.Email;
                dietitianName = string.IsNullOrEmpty(n) ? null : $"Dr. {n}";
            }
        }

        return new ClientDietProgramDayDto
        {
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
            HasProgram = true,
            UpdatedAt = p.UpdatedAt,
            DietitianName = dietitianName,
            BreakfastCompleted = p.BreakfastCompleted,
            LunchCompleted = p.LunchCompleted,
            DinnerCompleted = p.DinnerCompleted,
            SnackCompleted = p.SnackCompleted
        };
    }
}
