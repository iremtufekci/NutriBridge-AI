using Nightbrate.Application.DTOs;

namespace Nightbrate.Application.Interfaces;

public interface ISystemAnalyticsService
{
    Task<SystemAnalyticsDto> GetAsync();
}
