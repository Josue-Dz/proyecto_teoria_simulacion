import apiClient from "../apiClient";

export const runSimulationRequest = async (simulationRequest) => {
    const response = await apiClient.post("/api/simulations/run", simulationRequest);
    return response.data;
};

export const getUncertaintyRequest = async (base, runs = 200, transmissionVariability = 0.1) => {
    const response = await apiClient.post("/api/simulations/uncertainty", {
        base,
        runs,
        transmissionVariability,
    });
    return response.data;
};

export const compareScenariosRequest = async (scenarios) => {
    const response = await apiClient.post("/api/simulations/compare", { scenarios });
    return response.data;
};

export const getSimulationRequest = async (id) => {
    const response = await apiClient.get(`/api/simulations/${id}`);
    return response.data;
};

export const buildExportCsvUrl = (id) =>
    `${apiClient.defaults.baseURL ?? ""}/api/simulations/${id}/export`;
