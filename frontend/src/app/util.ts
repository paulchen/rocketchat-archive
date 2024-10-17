import {Channel, ChannelData} from "./channel-data";

export function sortChannels(channelData: ChannelData): ChannelData {
  let channels: Channel[] = [];
  channelData.favouriteChannels.forEach(channelName => {
    let channel = channelData.channels.find(channel => channel.name == channelName);
    if (channel !== undefined) {
      channels.push(channel);
    }
  });
  channels.push(...(channelData.channels.filter(channel => channelData.favouriteChannels.indexOf(channel.name) == -1)))

  return { channels: channels, favouriteChannels: channelData.favouriteChannels };
}
