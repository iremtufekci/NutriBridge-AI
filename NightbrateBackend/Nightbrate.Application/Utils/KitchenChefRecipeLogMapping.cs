using Nightbrate.Application.DTOs;
using Nightbrate.Core.Entities;

namespace Nightbrate.Application.Utils;

public static class KitchenChefRecipeLogMapping
{
    public static KitchenChefRecipeLogItemDto ToItemDto(KitchenChefRecipeLog e) =>
        new()
        {
            Id = e.Id,
            CreatedAtUtc = e.CreatedAtUtc,
            Ingredients = e.Ingredients,
            Preference = e.Preference,
            TargetCalories = e.TargetCalories,
            Source = e.Source,
            SelectedRecipes = e.SelectedRecipes.Select(s => new KitchenChefRecipeDto
            {
                Title = s.Title,
                Description = s.Description,
                EstimatedCalories = s.EstimatedCalories,
                PrepTimeMinutes = s.PrepTimeMinutes,
                Ingredients = s.Ingredients.ToList(),
                Steps = s.Steps.ToList()
            }).ToList()
        };
}
