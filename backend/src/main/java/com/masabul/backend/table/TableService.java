package com.masabul.backend.table;

import com.masabul.backend.security.AuthenticatedUserPrincipal;
import com.masabul.backend.user.User;
import com.masabul.backend.user.UserService;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableService {

    private static final int SRID_WGS84 = 4326;
    private static final int MAX_RADIUS_METERS = 50_000;
    private static final int MAX_LIMIT = 100;

    private final OkeyTableRepository tableRepository;
    private final UserService userService;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    public TableService(OkeyTableRepository tableRepository, UserService userService) {
        this.tableRepository = tableRepository;
        this.userService = userService;
    }

    @Transactional
    public TableResponse createTable(AuthenticatedUserPrincipal principal, CreateTableRequest request) {
        User host = userService.getCurrentUser(principal);
        if (tableRepository.existsByHostAndStatus(host, OkeyTableStatus.OPEN)) {
            throw new IllegalStateException("Host already has an active open table");
        }

        Point point = point(request.longitude(), request.latitude());
        OkeyTable table = new OkeyTable(
            host,
            request.title().trim(),
            request.venueName().trim(),
            point
        );

        return TableResponse.from(tableRepository.save(table));
    }

    @Transactional(readOnly = true)
    public List<NearbyTableResponse> findNearby(double latitude, double longitude, int radiusMeters, int limit) {
        validateLatitude(latitude);
        validateLongitude(longitude);
        if (radiusMeters < 1 || radiusMeters > MAX_RADIUS_METERS) {
            throw new IllegalArgumentException("radius must be between 1 and 50000 meters");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }

        return tableRepository.findNearbyOpenTables(latitude, longitude, radiusMeters, limit)
            .stream()
            .map(NearbyTableResponse::from)
            .toList();
    }

    private Point point(double longitude, double latitude) {
        validateLongitude(longitude);
        validateLatitude(latitude);
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(SRID_WGS84);
        return point;
    }

    private void validateLatitude(double latitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
    }

    private void validateLongitude(double longitude) {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }
}
