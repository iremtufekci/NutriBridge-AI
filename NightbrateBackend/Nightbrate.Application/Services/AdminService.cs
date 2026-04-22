using Nightbrate.Application.DTOs;
using Nightbrate.Application.Exceptions;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Services;

public class AdminService(
    IDietitianRepository dietitianRepository,
    IUserRepository userRepository,
    IClientRepository clientRepository) : IAdminService
{
    public Task<List<Dietitian>> GetPendingDietitiansAsync() => dietitianRepository.GetPendingAsync();

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

    public async Task ApproveDietitianAsync(string dietitianId)
    {
        var dietitian = await dietitianRepository.GetByIdAsync(dietitianId);
        if (dietitian is null) throw new AppException("Diyetisyen bulunamadı.");

        dietitian.IsApproved = true;
        await dietitianRepository.UpdateAsync(dietitian);
        if (dietitian.Id is not null)
        {
            await userRepository.SetDietitianIsApprovedInUsersCollectionAsync(dietitian.Id, isApproved: true);
        }
    }

    public async Task<DashboardStatsDto> GetStatsAsync()
    {
        var totalDietitians = await dietitianRepository.GetTotalAsync();
        var activeDietitians = await dietitianRepository.GetApprovedCountAsync();
        return new DashboardStatsDto
        {
            TotalUsers = await userRepository.GetTotalUsersAsync(),
            TotalClients = await clientRepository.GetTotalAsync(),
            TotalDietitians = totalDietitians,
            ActiveDietitians = activeDietitians,
            PendingDietitians = totalDietitians - activeDietitians
        };
    }
}
