import 'package:flutter_test/flutter_test.dart';
import 'package:masabul/features/auth/data/user_profile.dart';

void main() {
  test('parses user profile json', () {
    final profile = UserProfile.fromJson({
      'id': '11111111-1111-1111-1111-111111111111',
      'firebaseUid': 'firebase-1',
      'phone': '+905551112233',
      'fullName': 'MasaBul Kullanıcısı',
      'avatarUrl': null,
      'profileCompleted': false,
      'createdAt': '2026-09-14T12:00:00Z',
    });

    expect(profile.firebaseUid, 'firebase-1');
    expect(profile.profileCompleted, isFalse);
  });
}
