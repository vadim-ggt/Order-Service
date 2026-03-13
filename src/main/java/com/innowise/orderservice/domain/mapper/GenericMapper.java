package com.innowise.orderservice.domain.mapper;


import java.util.List;
import org.mapstruct.Builder;
import org.mapstruct.MapperConfig;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;


@MapperConfig(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
)
public interface GenericMapper<E,D> {

    D toDto(E e);

    E toEntity(D dto);

    List<D> toDtoList(Iterable<E> list);

    List<E> toEntityList(Iterable<D> list);

    E merge(@MappingTarget E entity, D dto);

}
