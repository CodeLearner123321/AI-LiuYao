package com.divination.liuyao.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserPermissionDTO {
    private Integer role;
    private List<String> viewPermissions;

}