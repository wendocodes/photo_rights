package com.klix.backend.viewmodel;

import java.sql.Date;
import com.klix.backend.validators.interfaces.FieldsValueMatch;
import com.klix.backend.model.User;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@NoArgsConstructor
@FieldsValueMatch.List({ 
    @FieldsValueMatch(
      field = "password",
      fieldMatch = "passwordConfirm",
      containsPassword = true,
      message = "{root.validator.userValidator.passwordsNotIdenical}"
    )
  })
  public class UserPasswordResetViewModel extends User
    {
        private Long id;
        private Long UserId;
        private String email;
        private String passwordToken;
        private Date expiryDate;

    }
