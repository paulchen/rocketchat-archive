const { writeFileSync } = require('fs');

const rocketchatUrl = process.env.ROCKETCHAT_URL;
const clientConfiguration = {
    rocketchatUrl: rocketchatUrl,
}

const clientConfigurationJson = JSON.stringify(clientConfiguration, null, 2);

writeFileSync('src/client-configuration.json', clientConfigurationJson);
