let postForm;
const BASE_URL = window.location.origin;
// Fetch and display all chirps when the page loads
document.addEventListener("DOMContentLoaded", function () {
    postForm = document.querySelector("#post-form");
    fetchAndDisplayChirps();
    
    postForm.addEventListener("submit", function (event) {
    event.preventDefault(); // Prevent form default submission
    postChirps();
    });

    const timeline = document.querySelector("#timeline");
    timeline.addEventListener("click", async (event) => {
        if (event.target.classList.contains("delete")) {
            const chirpID = event.target.dataset.id;
            await deleteChirp(chirpID);
        } else if (event.target.classList.contains("edit")) {
            const chirpElement = event.target.parentElement;
            const chirpID = event.target.dataset.id;
            editChirp(chirpID, chirpElement);
        }
    });

});



function postChirps() {
    const userName = document.querySelector("#username").value.trim();
    const content = document.querySelector("#content").value.trim();

    if (!userName || !content) {
        alert("Username and content cannot be empty!");
        return;
    }

    fetch(BASE_URL + "/chirps", {
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
    fetch(BASE_URL + "/chirps", { // Fetch from correct endpoint
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
            alert("Error fetching chirpsdwdddwwddww: " + error.message); // User feedback
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
            <button data-id=${chirp.id} class="edit">Edit</button>
            <button data-id=${chirp.id} class="delete">Delete</button>
        `;
        timeline.appendChild(chirpDiv); // Append each chirp to the timeline
    });
}

async function deleteChirp(chirpID) {
    try {
        const response = await fetch(BASE_URL + "/chirps" +`/${chirpID}`, {
            method: "DELETE"
        });

        if (!response.ok) {
            throw new Error(`Failed to delete chirp: ${response.statusText}`);
        }

        console.log(`Chirp with ID ${chirpID} deleted successfully.`);
        fetchAndDisplayChirps(); // Refresh the timeline after deletion
    } catch (error) {
        console.error(error.message);
        alert("Error deleting chirp: " + error.message); // User feedback
    }
}

/**
 * 
 * @param {*} chirpID 
 * @param {HTMLElement} chirpElement 
 */
function editChirp(chirpID, chirpElement) {
    const oldUserName = chirpElement.getElementsByClassName("username")[0].textContent || "";
    const oldContent = chirpElement.getElementsByClassName("content")[0].textContent || "";

    const newUserName = prompt("Enter the new username:", oldUserName);
    const newContent = prompt("Enter the new chirp content:", oldContent);

    if (newUserName === null || newContent === null) {
        alert("Update canceled");
        return;
    }
    // Check if inputs are valid
    if (newUserName.trim() === "" || newContent.trim() === "") {
        alert("Update canceled. Username and content cannot be empty.");
        return;
    }

    fetch(BASE_URL + "/chirps" +`/${chirpID}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ username: newUserName.trim(), content: newContent.trim() })
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("Failed to update the chirp on the server.");
            }
            return response.json();
        })
        .then(() => {
            // Refresh chirp display after successful update
            fetchAndDisplayChirps();
        })
        .catch(error => {
            console.error("Error updating chirp:", error);
            alert("There was an error updating the chirp. Please try again later.");
        });
}
