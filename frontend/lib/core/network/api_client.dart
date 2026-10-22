import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../errors/api_exception.dart';
import 'auth_interceptor.dart';

const apiBaseUrl = String.fromEnvironment(
  'MASABUL_API_BASE_URL',
  defaultValue: 'http://localhost:8080',
);

final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(
    BaseOptions(
      baseUrl: apiBaseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 20),
      sendTimeout: const Duration(seconds: 20),
      contentType: Headers.jsonContentType,
      responseType: ResponseType.json,
    ),
  );

  dio.interceptors.add(AuthInterceptor());
  dio.interceptors.add(
    InterceptorsWrapper(
      onError: (error, handler) => handler.reject(_mapDioError(error)),
    ),
  );

  return dio;
});

DioException _mapDioError(DioException error) {
  final response = error.response;
  final data = response?.data;

  if (data is Map<String, dynamic>) {
    final validationErrors = data['validationErrors'];
    return error.copyWith(
      error: ApiException(
        data['message'] as String? ?? 'İstek başarısız oldu.',
        statusCode: response?.statusCode,
        validationErrors: validationErrors is List
            ? validationErrors.whereType<String>().toList()
            : const [],
      ),
    );
  }

  return error.copyWith(
    error: ApiException(
      response == null ? 'Sunucuya ulaşılamadı.' : 'İstek başarısız oldu.',
      statusCode: response?.statusCode,
    ),
  );
}
