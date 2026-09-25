import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/user_profile.dart';
import '../data/user_repository.dart';

final currentUserProvider = FutureProvider.autoDispose<UserProfile>((ref) {
  return ref.watch(userRepositoryProvider).me();
});
