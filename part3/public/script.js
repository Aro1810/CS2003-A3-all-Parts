let postForm;
// Fetch and display all chirps when the page loads
document.addEventListener("DOMContentLoaded", function () {
    postForm = document.querySelector("#post-form");
    fetchAndDisplayChirps();
    
    postForm.addEventListener("submit", function (event) {
    event.preventDefault(); // Prevent form default submission
    postChirps();
});
});



function postChirps() {
    const userName = document.querySelector("#username").value.trim();
    const content = document.querySelector("#content").value.trim();

    if (!userName || !content) {
        alert("Username and content cannot be empty!");
        return;
    }

    fetch("http://localhost:24477/chirps", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ username: userName, content })
    })
        .then((response) => {
            if (!response.ok) {
                throw new Error(`Failed to post chirp: ${response.statusText}`);
            }
            return response.json();
        })
        .then((data) => {
            console.log("Chirp posted successfully:", data);
            document.querySelector("#username").value = ""; // Clear form inputs
            document.querySelector("#content").value = "";
            fetchAndDisplayChirps(); // Refresh the timeline with all chirps
        })
        .catch((error) => {
            console.error(error.message);
            alert("Error posting chirp: " + error.message); // User feedback
        });
}

function fetchAndDisplayChirps() {
    fetch("http://localhost:24477/chirps", { // Fetch from correct endpoint
        method: "GET",
        headers: {
            "Accept": "application/json"
        }
    })
        .then((response) => {
            if (!response.ok) {
                throw new Error(`Failed to fetch chirps: ${response.statusText}`);
            }
            return response.json();
        })
        .then((data) => {
            if (Array.isArray(data.chirps) && data.chirps.length > 0) {
                displayChirps(data.chirps);
            } else {
                const timeline = document.querySelector("#timeline");
                timeline.innerHTML = "<p>No chirps available. Be the first to post!</p>";
            }
        })
        .catch((error) => {
            console.error(error.message);
            alert("Error fetching chirps: " + error.message); // User feedback
        });
}

function displayChirps(chirps) {
    const timeline = document.querySelector("#timeline");
    timeline.innerHTML = ""; // Clear the timeline before appending new chirps

    chirps.forEach((chirp) => {
        const chirpDiv = document.createElement("div");
        chirpDiv.classList.add("chirp");
        chirpDiv.innerHTML = `
            <h3 class="username">${chirp.username}</h3>
            <p class="content">${chirp.content}</p>
            <p class="posted_at">Posted at: ${new Date(chirp.posted_at).toLocaleString()}</p>
        `;
        timeline.appendChild(chirpDiv); // Append each chirp to the timeline
    });
}