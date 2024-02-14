const config = {
    routingApi: location.origin + '/',
    geocodingApi: '',
    defaultTiles: 'Ordnance Survey Light',
    keys: {
        graphhopper: "",
        maptiler: "missing_api_key",
        omniscale: "missing_api_key",
        thunderforest: "missing_api_key",
        kurviger: "missing_api_key",
        ordnancesurvey: 'LtYJq12RX8nMZWHt1ADf3EAc2LdBiZX6',
    },
    routingGraphLayerAllowed: true,
    request: {
        details: [
            'road_class',
            'road_environment',
            'max_speed',
            'average_speed',
        ],
        snapPreventions: ['ferry'],
    },
}