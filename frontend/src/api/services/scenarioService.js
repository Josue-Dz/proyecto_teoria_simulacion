import apiClient from "../apiClient";

export const getScenariosRequest = async () => {
    const response = await apiClient.get("/api/scenarios");
    return response.data;
};

export const getScenarioRequest = async (id) => {
    const response = await apiClient.get(`/api/scenarios/${id}`);
    return response.data;
};

export const createScenarioRequest = async (scenarioData) => {
    const response = await apiClient.post("/api/scenarios", scenarioData);
    return response.data;
};

export const deleteScenarioRequest = async (id) => {
    const response = await apiClient.delete(`/api/scenarios/${id}`);
    return response.data;
};

export const runScenarioRequest = async (id) => {
    const response = await apiClient.post(`/api/scenarios/${id}/run`);
    return response.data;
};
