import apiClient from './apiClient';

//11.1 GET /api/worker/members?search= -> [{ memberId, name, surname, email, membershipActive }]
export async function getMembers(search) {
    const params = search ? { search } : {};
    const response = await apiClient.get('/worker/members', { params });
    return Array.isArray(response.data) ? response.data : [];
}

//11.2 GET /api/worker/members/{memberId}
export async function getMember(memberId) {
   const response = await apiClient.get('/worker/members',data);
   return response.data;    
}

//11.3 POST /api/worker/members -> 201 {memberId, temporaryPassword, message }
export async function  addMember(data) {
    const response = await apiClient.post('/worker/members',data);
    return response.data;
}

//11.4 DELETE /api/worker/members/{memberId} -> { message }
export async function  deleteMember {memberId} {
    const response = await apiClient.delete('/worker/members/${memberId}');
    return response.data;
}
