class TableHost {
  const TableHost({
    required this.id,
    required this.fullName,
    this.avatarUrl,
  });

  final String id;
  final String fullName;
  final String? avatarUrl;

  factory TableHost.fromJson(Map<String, dynamic> json) {
    return TableHost(
      id: json['id'] as String,
      fullName: json['fullName'] as String,
      avatarUrl: json['avatarUrl'] as String?,
    );
  }
}

class NearbyTable {
  const NearbyTable({
    required this.id,
    required this.title,
    required this.venueName,
    required this.host,
    required this.latitude,
    required this.longitude,
    required this.distanceMeters,
    required this.availableSpots,
    required this.createdAt,
  });

  final String id;
  final String title;
  final String venueName;
  final TableHost host;
  final double latitude;
  final double longitude;
  final double distanceMeters;
  final int availableSpots;
  final DateTime createdAt;

  factory NearbyTable.fromJson(Map<String, dynamic> json) {
    return NearbyTable(
      id: json['id'] as String,
      title: json['title'] as String,
      venueName: json['venueName'] as String,
      host: TableHost.fromJson(json['host'] as Map<String, dynamic>),
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      distanceMeters: (json['distanceMeters'] as num).toDouble(),
      availableSpots: json['availableSpots'] as int,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}

class TableDetails {
  const TableDetails({
    required this.id,
    required this.title,
    required this.venueName,
    required this.host,
    required this.latitude,
    required this.longitude,
    required this.currentPlayers,
    required this.maxPlayers,
    required this.availableSpots,
    required this.status,
    required this.createdAt,
  });

  final String id;
  final String title;
  final String venueName;
  final TableHost host;
  final double latitude;
  final double longitude;
  final int currentPlayers;
  final int maxPlayers;
  final int availableSpots;
  final String status;
  final DateTime createdAt;

  bool get isOpen => status == 'OPEN';

  factory TableDetails.fromJson(Map<String, dynamic> json) {
    return TableDetails(
      id: json['id'] as String,
      title: json['title'] as String,
      venueName: json['venueName'] as String,
      host: TableHost.fromJson(json['host'] as Map<String, dynamic>),
      latitude: (json['latitude'] as num).toDouble(),
      longitude: (json['longitude'] as num).toDouble(),
      currentPlayers: json['currentPlayers'] as int,
      maxPlayers: json['maxPlayers'] as int,
      availableSpots: json['availableSpots'] as int,
      status: json['status'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}

class TableJoinRequest {
  const TableJoinRequest({
    required this.requestId,
    required this.tableId,
    required this.user,
    required this.status,
    required this.createdAt,
    this.respondedAt,
  });

  final String requestId;
  final String tableId;
  final TableHost user;
  final String status;
  final DateTime createdAt;
  final DateTime? respondedAt;

  bool get isPending => status == 'PENDING';

  factory TableJoinRequest.fromJson(Map<String, dynamic> json) {
    return TableJoinRequest(
      requestId: json['requestId'] as String,
      tableId: json['tableId'] as String,
      user: TableHost.fromJson(json['user'] as Map<String, dynamic>),
      status: json['status'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      respondedAt: json['respondedAt'] == null
          ? null
          : DateTime.parse(json['respondedAt'] as String),
    );
  }
}
