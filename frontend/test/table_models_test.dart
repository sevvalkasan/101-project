import 'package:flutter_test/flutter_test.dart';
import 'package:masabul/features/tables/data/table_models.dart';

void main() {
  test('parses nearby table coordinates and capacity', () {
    final table = NearbyTable.fromJson({
      'id': '11111111-1111-1111-1111-111111111111',
      'title': 'Akşam 101',
      'venueName': 'Moda Cafe',
      'host': {
        'id': '22222222-2222-2222-2222-222222222222',
        'fullName': 'Sevval Kasan',
        'avatarUrl': null,
      },
      'latitude': 40.987,
      'longitude': 29.037,
      'distanceMeters': 245.7,
      'availableSpots': 3,
      'createdAt': '2026-09-29T00:00:00Z',
    });

    expect(table.latitude, 40.987);
    expect(table.longitude, 29.037);
    expect(table.availableSpots, 3);
    expect(table.host.fullName, 'Sevval Kasan');
  });

  test('parses active table status', () {
    final table = TableDetails.fromJson({
      'id': '11111111-1111-1111-1111-111111111111',
      'title': 'Akşam 101',
      'venueName': 'Moda Cafe',
      'host': {
        'id': '22222222-2222-2222-2222-222222222222',
        'fullName': 'Sevval Kasan',
        'avatarUrl': null,
      },
      'latitude': 40.987,
      'longitude': 29.037,
      'currentPlayers': 4,
      'maxPlayers': 4,
      'availableSpots': 0,
      'status': 'FULL',
      'createdAt': '2026-09-29T00:00:00Z',
    });

    expect(table.isOpen, isFalse);
    expect(table.availableSpots, 0);
  });
}
