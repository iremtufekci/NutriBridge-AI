using MongoDB.Bson; // ObjectId üretimi
using MongoDB.Driver; // MongoDB sürücü API'si
using Nightbrate.Application.Interfaces; // IDietProgramHistoryRepository arayüzü
using Nightbrate.Core.Entities; // DietProgram, DietProgramHistory varlıkları
using Nightbrate.Infrastructure.Data; // MongoDbContext

namespace Nightbrate.Infrastructure.Repositories; // Veri erişim katmanı depoları

public class DietProgramHistoryRepository(MongoDbContext context) : IDietProgramHistoryRepository // Program geçmişi arşivi
{
    public Task ArchiveCurrentBeforeUpdateAsync(DietProgram previousCurrent, DateTime supersededAtUtc) // Güncellemeden önce eski sürümü arşivle
    {
        var doc = new DietProgramHistory // Arşiv belgesi oluştur
        {
            Id = ObjectId.GenerateNewId().ToString(), // Yeni arşiv id
            ClientId = previousCurrent.ClientId, // Danışan
            DietitianId = previousCurrent.DietitianId, // Diyetisyen
            ProgramDate = previousCurrent.ProgramDate, // Program günü
            Breakfast = previousCurrent.Breakfast, // Eski kahvaltı
            Lunch = previousCurrent.Lunch, // Eski öğle
            Dinner = previousCurrent.Dinner, // Eski akşam
            Snack = previousCurrent.Snack, // Eski ara öğün
            BreakfastCalories = previousCurrent.BreakfastCalories,
            LunchCalories = previousCurrent.LunchCalories,
            DinnerCalories = previousCurrent.DinnerCalories,
            SnackCalories = previousCurrent.SnackCalories,
            TotalCalories = previousCurrent.TotalCalories,
            ProgramContentUpdatedAt = previousCurrent.UpdatedAt, // Eski sürümün güncellenme zamanı
            SupersededAt = supersededAtUtc, // Değiştirilme anı
            SupersededDietProgramId = previousCurrent.Id // Eski program id
        };
        return context.DietProgramHistories.InsertOneAsync(doc); // Arşive ekle
    }
}
