using MongoDB.Bson;
using MongoDB.Driver;
using Nightbrate.Application.Interfaces;
using Nightbrate.Core.Entities;
using Nightbrate.Infrastructure.Data;

namespace Nightbrate.Infrastructure.Repositories;

public class DietProgramHistoryRepository(MongoDbContext context) : IDietProgramHistoryRepository
{
    public Task ArchiveCurrentBeforeUpdateAsync(DietProgram previousCurrent, DateTime supersededAtUtc)
    {
        var doc = new DietProgramHistory
        {
            Id = ObjectId.GenerateNewId().ToString(),
            ClientId = previousCurrent.ClientId,
            DietitianId = previousCurrent.DietitianId,
            ProgramDate = previousCurrent.ProgramDate,
            Breakfast = previousCurrent.Breakfast,
            Lunch = previousCurrent.Lunch,
            Dinner = previousCurrent.Dinner,
            Snack = previousCurrent.Snack,
            BreakfastCalories = previousCurrent.BreakfastCalories,
            LunchCalories = previousCurrent.LunchCalories,
            DinnerCalories = previousCurrent.DinnerCalories,
            SnackCalories = previousCurrent.SnackCalories,
            TotalCalories = previousCurrent.TotalCalories,
            ProgramContentUpdatedAt = previousCurrent.UpdatedAt,
            SupersededAt = supersededAtUtc,
            SupersededDietProgramId = previousCurrent.Id
        };
        return context.DietProgramHistories.InsertOneAsync(doc);
    }
}
