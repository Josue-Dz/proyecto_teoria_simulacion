import axios from "axios";

const apiClient = axios.create({
    baseURL: `${import.meta.env.VITE_API_URL}`,
    headers: {
        "Content-Type": "application/json",
    },
    timeout: 0,
});

apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response) {
            const { status, statusText, data } = error.response;
            return Promise.reject(new Error(data?.message || `${status} ${statusText}`));
        }
        if (error.request) {
            return Promise.reject(
                new Error("No hay respuesta del servidor. ¿Está corriendo el backend?")
            );
        }
        return Promise.reject(error);
    }
);

export default apiClient;
