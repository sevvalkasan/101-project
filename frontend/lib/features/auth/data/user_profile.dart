class UserProfile {
  const UserProfile({
    required this.id,
    required this.firebaseUid,
    required this.phone,
    required this.fullName,
    required this.profileCompleted,
    required this.createdAt,
    this.avatarUrl,
  });

  final String id;
  final String firebaseUid;
  final String phone;
  final String fullName;
  final String? avatarUrl;
  final bool profileCompleted;
  final DateTime createdAt;

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id'] as String,
      firebaseUid: json['firebaseUid'] as String,
      phone: json['phone'] as String,
      fullName: json['fullName'] as String,
      avatarUrl: json['avatarUrl'] as String?,
      profileCompleted: json['profileCompleted'] as bool,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}
