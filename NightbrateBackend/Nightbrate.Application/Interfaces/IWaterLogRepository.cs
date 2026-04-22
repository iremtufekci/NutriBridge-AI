using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IWaterLogRepository
{
    Task AddAsync(WaterLog waterLog);
}
