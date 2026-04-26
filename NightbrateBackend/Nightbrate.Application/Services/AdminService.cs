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
    private const string StatusActive = "active";
    private const string StatusPending = "pending";
    private const string StatusSuspended = "suspended";
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

    public async Task<UserManagementStatsDto> GetUserManagementStatsAsync()
    {
        var users = await userRepository.GetAllUsersForAdminAsync();
        return BuildStatsFrom(users);
    }

    public async Task<List<AdminUserRowDto>> GetUsersListAsync(
        string? search,
        string? roleFilter,
        string? statusFilter)
    {
        var users = await userRepository.GetAllUsersForAdminAsync();
        var lastMap = await activityLogService.GetLastActivityByUserIdsAsync(
            users.Select(x => x.Id!).Where(s => !string.IsNullOrEmpty(s)).ToList());

        var q = (search ?? "").Trim();
        var rf = (roleFilter ?? "all").Trim().ToLowerInvariant();
        var sf = (statusFilter ?? "all").Trim().ToLowerInvariant();

        IEnumerable<BaseUser> filtered = users;
        if (q.Length > 0)
        {
            var ql = q.ToLowerInvariant();
            filtered = users.Where(u => MatchesSearch(u, ql));
        }

        if (rf is not ("" or "all"))
        {
            filtered = filtered.Where(u =>
            {
                var r = u.Role switch
                {
                    UserRole.Admin => "admin",
                    UserRole.Dietitian => "dietitian",
                    UserRole.Client => "client",
                    _ => ""
                };
                return r == rf;
            });
        }

        if (sf is not ("" or "all"))
        {
            filtered = filtered.Where(u => GetStatusKey(u) == sf);
        }

        return filtered
            .OrderByDescending(x => x.CreatedAt)
            .Select(u => MapRow(u, lastMap))
            .ToList();
    }

    public async Task SuspendUserAsync(
        string targetUserId,
        string message,
        string? adminUserId,
        string adminDisplayName)
    {
        if (string.IsNullOrWhiteSpace(targetUserId)) throw new AppException("Kullanıcı bulunamadı.");
        var msg = (message ?? "").Trim();
        if (string.IsNullOrEmpty(msg)) throw new AppException("Askıya alma nedeni (mesaj) zorunludur.");

        if (!string.IsNullOrEmpty(adminUserId) &&
            string.Equals(targetUserId, adminUserId, StringComparison.Ordinal))
            throw new AppException("Kendi hesabınızı askıya alamazsınız.");

        var u = await userRepository.GetByIdAsync(targetUserId);
        if (u is null) throw new AppException("Kullanıcı bulunamadı.");

        await userRepository.SetUserSuspensionAllStoresAsync(
            targetUserId,
            isSuspended: true,
            message: msg,
            suspendedAt: DateTime.UtcNow);

        var adminName = string.IsNullOrWhiteSpace(adminDisplayName) ? "Yönetici" : adminDisplayName.Trim();
        await activityLogService.LogAsync(
            adminUserId,
            adminName,
            $"Kullanıcı {u.Email} askıya alındı: {msg}");
    }

    public async Task UnsuspendUserAsync(
        string targetUserId,
        string? adminUserId,
        string adminDisplayName)
    {
        if (string.IsNullOrWhiteSpace(targetUserId)) throw new AppException("Kullanıcı bulunamadı.");

        if (!string.IsNullOrEmpty(adminUserId) &&
            string.Equals(targetUserId, adminUserId, StringComparison.Ordinal))
            throw new AppException("Kendi hesabınız üzerinde bu işlemi kullanmayın.");

        var u = await userRepository.GetByIdAsync(targetUserId);
        if (u is null) throw new AppException("Kullanıcı bulunamadı.");

        await userRepository.SetUserSuspensionAllStoresAsync(
            targetUserId,
            isSuspended: false,
            message: null,
            suspendedAt: null);

        var adminName = string.IsNullOrWhiteSpace(adminDisplayName) ? "Yönetici" : adminDisplayName.Trim();
        await activityLogService.LogAsync(
            adminUserId,
            adminName,
            $"Kullanıcı {u.Email} askıdan kaldırıldı");
    }

    private static UserManagementStatsDto BuildStatsFrom(IReadOnlyList<BaseUser> users)
    {
        var dto = new UserManagementStatsDto
        {
            TotalUsers = users.Count,
            Admins = users.Count(x => x.Role == UserRole.Admin),
            Dietitians = users.Count(x => x.Role == UserRole.Dietitian),
            Clients = users.Count(x => x.Role == UserRole.Client),
            Active = 0,
            Pending = 0
        };

        foreach (var u in users)
        {
            if (u is Dietitian d && !d.IsApproved) dto.Pending++;
            if (!u.IsSuspended && (u is not Dietitian d2 || d2.IsApproved)) dto.Active++;
        }

        return dto;
    }

    private static bool MatchesSearch(BaseUser u, string ql)
    {
        if (u.Email.ToLowerInvariant().Contains(ql)) return true;
        if (!string.IsNullOrEmpty(u.Phone) && u.Phone.Replace(" ", "", StringComparison.Ordinal).Contains(ql, StringComparison.OrdinalIgnoreCase)) return true;

        if (u is Client c)
        {
            var n = ($"{c.FirstName} {c.LastName}").Trim().ToLowerInvariant();
            if (n.Length > 0 && n.Contains(ql, StringComparison.Ordinal)) return true;
        }
        else if (u is Dietitian d)
        {
            var n = ($"Dr. {d.FirstName} {d.LastName}").Trim().ToLowerInvariant();
            if (n.Length > 0 && n.Contains(ql, StringComparison.Ordinal)) return true;
            var n2 = ($"{d.FirstName} {d.LastName}").Trim().ToLowerInvariant();
            if (n2.Length > 0 && n2.Contains(ql, StringComparison.Ordinal)) return true;
        }

        return false;
    }

    private static string GetStatusKey(BaseUser u)
    {
        if (u.IsSuspended) return StatusSuspended;
        if (u is Dietitian d && !d.IsApproved) return StatusPending;
        return StatusActive;
    }

    private static AdminUserRowDto MapRow(BaseUser u, IReadOnlyDictionary<string, DateTime> lastMap)
    {
        var (display, initial) = GetDisplayAndInitial(u);
        var roleKey = u.Role switch
        {
            UserRole.Admin => "Admin",
            UserRole.Dietitian => "Diyetisyen",
            UserRole.Client => "Danışan",
            _ => u.Role.ToString()
        };
        var rk = u.Role switch
        {
            UserRole.Admin => "admin",
            UserRole.Dietitian => "dietitian",
            UserRole.Client => "client",
            _ => "client"
        };
        var sk = GetStatusKey(u);
        var sl = sk switch
        {
            _ when sk == StatusSuspended => "Askıda",
            _ when sk == StatusPending => "Beklemede",
            _ => "Aktif"
        };

        string? lastIso = null;
        if (!string.IsNullOrEmpty(u.Id) && lastMap.TryGetValue(u.Id, out var t))
            lastIso = t.ToUniversalTime().ToString("O");

        return new AdminUserRowDto
        {
            Id = u.Id ?? string.Empty,
            DisplayName = display,
            Initial = initial,
            Email = u.Email,
            Phone = string.IsNullOrWhiteSpace(u.Phone) ? "—" : u.Phone,
            Role = roleKey,
            RoleKey = rk,
            StatusKey = sk,
            StatusLabel = sl,
            CreatedAt = u.CreatedAt,
            LastActivityAt = lastIso,
            IsSuspended = u.IsSuspended
        };
    }

    private static (string Display, string Initial) GetDisplayAndInitial(BaseUser u)
    {
        if (u is Admin a)
        {
            var n = $"{a.FirstName} {a.LastName}".Trim();
            if (n.Length == 0) n = u.Email;
            return (n, FirstLetter(n));
        }
        if (u is Client c)
        {
            var n = $"{c.FirstName} {c.LastName}".Trim();
            if (n.Length == 0) n = c.Email;
            return (n, FirstLetter(n));
        }
        if (u is Dietitian d)
        {
            var n = $"Dr. {d.FirstName} {d.LastName}".Trim();
            if (n.Length < 4) n = d.Email;
            return (n, FirstLetter(n));
        }
        var em = u.Email;
        if (em.Contains('@', StringComparison.Ordinal)) return (em, FirstLetter(em.Split('@')[0]));
        return (em, FirstLetter(em));
    }

    private static string FirstLetter(string s)
    {
        s = s.Trim();
        if (s.Length == 0) return "?";
        foreach (var ch in s)
        {
            if (char.IsLetterOrDigit(ch)) return ch.ToString().ToUpperInvariant();
        }
        return s[0].ToString().ToUpperInvariant();
    }
}
