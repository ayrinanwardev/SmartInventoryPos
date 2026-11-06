package smart_inventory_backend.common

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

private const val DEFAULT_PAGE_SIZE = 50
private const val MAX_PAGE_SIZE = 200

/** Paging is opt-in: with neither value given, callers get the full list, as existing clients expect. */
fun pageableOf(page: Int?, size: Int?): Pageable =
    if (page == null && size == null) {
        Pageable.unpaged()
    } else {
        PageRequest.of((page ?: 0).coerceAtLeast(0), (size ?: DEFAULT_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE))
    }
