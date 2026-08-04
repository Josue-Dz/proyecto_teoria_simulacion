import apiClient from "../apiClient";

export const getModelDefaultsRequest = async () => {
    const response = await apiClient.get("/api/model/defaults");
    return response.data;
};
