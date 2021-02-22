package ovh.plrapps.mapview.core

/**
 * Marks declarations that are **internal** in MapView API, which means that should not be used outside of
 * `ovh.plrapps.mapview`, because their signatures and semantics will change between future releases without any
 * warnings and without providing any migration aids.
 */
@Retention(value = AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS, AnnotationTarget.PROPERTY)
annotation class InternalMapViewApi