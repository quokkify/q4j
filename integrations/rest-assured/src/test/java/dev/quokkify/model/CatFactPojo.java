package dev.quokkify.model;

import com.github.reinert.jjschema.Attributes;

public record CatFactPojo(
    @Attributes(required = true) String fact,
    @Attributes(required = true) Integer length
) {

}
