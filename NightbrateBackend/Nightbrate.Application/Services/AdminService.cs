using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Application.Utils;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class AdminService(
    IDietitianRepository dietitianRepository,
    IUserRepository userRepository,
    IClientRepository clientRepository,
    IActivityLogService activityLogService) : IAdminService
{
    public async Task<List<PendingDietitianListItemDto>> GetPendingDietitiansAsync()
    {
        var list = await dietitianRepository.GetPendingAsync();
        return list
            .Select(d => new PendingDietitianListItemDto
            {
                Id = d.Id ?? string.Empty,
                FirstName = d.FirstName,
                LastName = d.LastName,
                Email = d.Email,
                DiplomaNo = d.DiplomaNo,
                ClinicName = d.ClinicName,
                CreatedAt = d.CreatedAt,
                IsApproved = d.IsApproved
            })
            .ToList();
    }

    public async Task<AdminDietitianDetailDto> GetDietitianDetailAsync(string dietitianId)
    {
        var dietitian = await dietitianRepository.GetByIdAsync(dietitianId);
        if (dietitian is null) throw new AppException("Diyetisyen bulunamadi.");

        return new AdminDietitianDetailDto
        {
            Id = dietitian.Id ?? string.Empty,
            FirstName = dietitian.FirstName,
            LastName = dietitian.LastName,
            DiplomaNo = dietitian.DiplomaNo,
            ClinicName = dietitian.ClinicName,
            CreatedAt = dietitian.CreatedAt,
            IsApproved = dietitian.IsApproved,
            DiplomaDocumentUrl = dietitian.DiplomaDocumentUrl
        };
    }

    public async Task ApproveDietitianAsync(string dietitianId, string? adminUserId, string adminDisplayName)
    {
        var dietitian = await dietitianRepository.GetByIdAsync(dietitianId);
        if (dietitian is null) throw new AppException("Diyetisyen bulunamadı.");

        dietitian.IsApproved = true;
        if (string.IsNullOrWhiteSpace(dietitian.ConnectionCode))
        {
            string newCode;
            do newCode = ConnectionCodeGenerator.NewCode();
            while (await dietitianRepository.ConnectionCodeExistsAsync(newCode));
            dietitian.ConnectionCode = newCode;
        }
        await dietitianRepository.UpdateAsync(dietitian);
        if (dietitian.Id is not null)
        {
            await userRepository.SetDietitianIsApprovedInUsersCollectionAsync(dietitian.Id, isApproved: true);
            if (!string.IsNullOrEmpty(dietitian.ConnectionCode))
            {
                await userRepository.SetDietitianConnectionCodeInUsersCollectionAsync(
                    dietitian.Id,
                    dietitian.ConnectionCode!);
            }
        }

        var adminName = string.IsNullOrWhiteSpace(adminDisplayName) ? "Yönetici" : adminDisplayName.Trim();
        await activityLogService.LogAsync(
            adminUserId,
            adminName,
            $"Dr. {dietitian.FirstName} {dietitian.LastName} adlı diyetisyen başvurusunu onayladı");
    }

    public async Task<DashboardStatsDto> GetStatsAsync()
    {
        var totalDietitians = await dietitianRepository.GetTotalAsync();
        var activeDietitians = await dietitianRepository.GetApprovedCountAsync();
        var totalUsers = await userRepository.GetTotalUsersAsync();
        var clientUsers = await userRepository.CountByRoleAsync(UserRole.Client);
        var adminUsers = await userRepository.CountByRoleAsync(UserRole.Admin);
        // Giris acik hesaplar: danisan, admin, onayli diyetisyen
        var activeUsers = clientUsers + adminUsers + activeDietitians;

        var roleDistribution = new List<RoleCountDto>
        {
            new() { Role = UserRole.Client, Count = clientUsers },
            new() { Role = UserRole.Dietitian, Count = await userRepository.CountByRoleAsync(UserRole.Dietitian) },
            new() { Role = UserRole.Admin, Count = adminUsers }
        };

        var monthly = await userRepository.GetMonthlyUserRegistrationsAsync(6);

        return new DashboardStatsDto
        {
            TotalUsers = totalUsers,
            ActiveUsers = activeUsers,
            TotalClients = await clientRepository.GetTotalAsync(),
            TotalDietitians = totalDietitians,
            ActiveDietitians = activeDietitians,
            PendingDietitians = totalDietitians - activeDietitians,
            RoleDistribution = roleDistribution,
            MonthlyRegistrations = monthly
        };
    }
}
