package com.sahay.third.party.object;


import com.sahay.third.party.model.Menu;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuDto {

    private String response;
    private String responseDescription;
    private List<Menu> menus;

}
