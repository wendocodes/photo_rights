package com.klix.backend.validators.groups;

import javax.validation.groups.Default;

/**
 * Defaults on passwords shold be to ignore them on @Valid with no group specified,
 * therefore using javax.validation.groups.Default if one should be validated, use
 * this group Example in User-model
 */
public interface ClearPassword extends Default { }
