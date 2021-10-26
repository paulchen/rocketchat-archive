export class ChannelData {
  channels: Channel[];
}

export class Channel {
  name: string;
  id: string;
}

export class ChannelStats {
  userMessageCount: MessageCount[];
  timebasedMessageCounts: {[key: string]: TimebasedMessageCount};
}

export class TimebasedMessageCount {
  messageCounts: MessageCount[];
}

export class MessageCount {
  key: string;
  messages: number;
}
