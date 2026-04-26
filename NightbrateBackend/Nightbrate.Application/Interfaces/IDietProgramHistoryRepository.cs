using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Interfaces;

public interface IDietProgramHistoryRepository
{
    /// <summary>Mevcut güncel <paramref name="previousCurrent"/> satırını arşiv koleksiyonuna yazar (güncelleme öncesi).</summary>
    Task ArchiveCurrentBeforeUpdateAsync(DietProgram previousCurrent, DateTime supersededAtUtc);
}
