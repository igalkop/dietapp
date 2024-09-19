package com.ikop.diet.controller;

import com.ikop.diet.model.FoodDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Foods {
    private List<FoodDTO> foods;
}
