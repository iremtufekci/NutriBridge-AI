using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IWeightLogRepository
{
    Task AddAsync(WeightLog weightLog);
}
