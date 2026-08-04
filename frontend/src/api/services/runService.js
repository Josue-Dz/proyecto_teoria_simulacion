import apiClient from "../apiClient";

export const getRunsRequest = async () => {
    const response = await apiClient.get("/api/runs");
    return response.data;
};

export const getRunsByScenarioRequest = async (scenarioId) => {
    const response = await apiClient.get(`/api/runs/scenario/${scenarioId}`);
    return response.data;
};
