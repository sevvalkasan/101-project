import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import 'table_models.dart';

final tableRepositoryProvider = Provider<TableRepository>((ref) {
  return TableRepository(ref.watch(dioProvider));
});

class TableRepository {
  const TableRepository(this._dio);

  final Dio _dio;

  Future<List<NearbyTable>> nearby({
    required double latitude,
    required double longitude,
    int radiusMeters = 5000,
    int limit = 50,
  }) async {
    final response = await _dio.get<List<dynamic>>(
      '/api/v1/tables/nearby',
      queryParameters: {
        'lat': latitude,
        'lng': longitude,
        'radius': radiusMeters,
        'limit': limit,
      },
    );

    return response.data!
        .map((item) => NearbyTable.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<TableDetails> create({
    required String title,
    required String venueName,
    required double latitude,
    required double longitude,
  }) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/api/v1/tables',
      data: {
        'title': title,
        'venueName': venueName,
        'latitude': latitude,
        'longitude': longitude,
      },
    );
    return TableDetails.fromJson(response.data!);
  }

  Future<TableDetails?> myActiveTable() async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/api/v1/tables/mine',
    );
    if (response.statusCode == 204 || response.data == null) {
      return null;
    }
    return TableDetails.fromJson(response.data!);
  }

  Future<List<TableDetails>> chatTables() async {
    final response = await _dio.get<List<dynamic>>('/api/v1/tables/chats');
    return response.data!
        .map((item) => TableDetails.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<void> sendJoinRequest(String tableId) async {
    await _dio.post<void>('/api/v1/tables/$tableId/requests');
  }

  Future<List<TableJoinRequest>> requests(String tableId) async {
    final response = await _dio.get<List<dynamic>>(
      '/api/v1/tables/$tableId/requests',
    );
    return response.data!
        .map((item) => TableJoinRequest.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<TableJoinRequest> respond({
    required String requestId,
    required bool accept,
  }) async {
    final response = await _dio.post<Map<String, dynamic>>(
      '/api/v1/requests/$requestId/respond',
      data: {'decision': accept ? 'ACCEPT' : 'REJECT'},
    );
    return TableJoinRequest.fromJson(response.data!);
  }

  Future<void> cancel(String tableId) async {
    await _dio.post<void>('/api/v1/tables/$tableId/cancel');
  }
}
