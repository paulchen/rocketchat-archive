export class ChannelData {
  channels: Channel[];
}

export class Channel {
  name: string;
  id: string;
}

export class ChannelStats {
  userMessageCount: {[userName: string]: number};
  timebasedMessageCounts: {[key: string]: TimebasedMessageCount};
}

export class TimebasedMessageCount {
  messageCounts: {[timeDefinition: string]: number};
}
