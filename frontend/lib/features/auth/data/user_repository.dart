import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import 'user_profile.dart';

final userRepositoryProvider = Provider<UserRepository>((ref) {
  return UserRepository(ref.watch(dioProvider));
});

class UserRepository {
  const UserRepository(this._dio);

  final Dio _dio;

  Future<UserProfile> me() async {
    final response = await _dio.get<Map<String, dynamic>>('/api/v1/users/me');
    return UserProfile.fromJson(response.data!);
  }

  Future<UserProfile> updateProfile({
    required String fullName,
    String? avatarUrl,
  }) async {
    final response = await _dio.put<Map<String, dynamic>>(
      '/api/v1/users/me',
      data: {
        'fullName': fullName,
        'avatarUrl': avatarUrl,
      },
    );
    return UserProfile.fromJson(response.data!);
  }
}
