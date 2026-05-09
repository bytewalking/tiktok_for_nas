package com.tiktoknas.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "app_setting")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppSetting {

    @Id
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;
}
