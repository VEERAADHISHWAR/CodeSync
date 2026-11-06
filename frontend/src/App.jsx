import { useState, useEffect } from 'react'
import client from './websocket'
import Editor from '@monaco-editor/react'
import './App.css'

function App() {

  const [roomId, setRoomId] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [joined, setJoined] = useState(false)

  const [versions, setVersions] = useState([])
  const [versionsLoading, setVersionsLoading] = useState(false)
  const [showVersionHistory, setShowVersionHistory] = useState(false)

  // Live collaborator presence
  const [participants, setParticipants] = useState([])
  const [participantId] = useState(
    () => Math.random().toString(36).slice(2)
  )
  const [displayName] = useState(
    () => `Guest-${Math.floor(1000 + Math.random() * 9000)}`
  )

  // Optional version metadata
  const [versionName, setVersionName] = useState('')
  const [versionDescription, setVersionDescription] = useState('')
  const [showVersionForm, setShowVersionForm] = useState(false)

  // Starter code for each programming language
  const starterCode = {
    java: `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, world!");
    }
}`,
    c: `#include <stdio.h>

int main() {
    printf("Hello, world!\\n");
    return 0;
}`,
    cpp: `#include <iostream>
using namespace std;

int main() {
    cout << "Hello, world!" << endl;
    return 0;
}`,
    python: `print("Hello, world!")`,
    javascript: `console.log("Hello, world!");`,
    html: `<!DOCTYPE html>
<html>
  <body>
    <h1>Hello, world!</h1>
  </body>
</html>`,
    css: `body {
    background-color: lightblue;
}`
  }

  const [language, setLanguage] = useState('java')
  const [code, setCode] = useState(starterCode.java)

  // Maintain a separate code draft for every language
  const [languageDrafts, setLanguageDrafts] = useState(
    () => ({ ...starterCode })
  )

  const [isExecuting, setIsExecuting] = useState(false)
  const [executionOutput, setExecutionOutput] = useState('')
  const [executionStatus, setExecutionStatus] = useState('')
  const [showPreview, setShowPreview] = useState(false)


  // Connect to WebSocket and manage room collaborator presence
  useEffect(() => {

    if (!joined || !roomId) {
      return
    }

    client.onConnect = () => {

      console.log('WebSocket connected!')

      // Subscribe to code and language updates for this room
      client.subscribe(`/topic/room/${roomId}`, (message) => {

        try {

          const receivedMessage = JSON.parse(message.body)

          const receivedLanguage =
            receivedMessage.language || 'java'

          const receivedCode =
            receivedMessage.code ?? ''

          // Update the draft belonging to the received language
          setLanguageDrafts(previousDrafts => ({
            ...previousDrafts,
            [receivedLanguage]: receivedCode
          }))

          setCode(receivedCode)
          setLanguage(receivedLanguage)

        } catch (error) {
          console.error('Could not read room update:', error)
        }

      })

      // Subscribe to the live participant list
      client.subscribe(`/topic/presence/${roomId}`, (message) => {

        try {
          setParticipants(JSON.parse(message.body))
        } catch (error) {
          console.error('Could not read collaborator presence:', error)
        }

      })

      // Subscribe to the initial participant snapshot
      client.subscribe(`/app/presence/${roomId}`, (message) => {

        try {
          setParticipants(JSON.parse(message.body))
        } catch (error) {
          console.error('Could not read participant snapshot:', error)
        }

      })

      // Announce this participant after subscriptions are active
      client.publish({

        destination: `/app/presence/${roomId}`,

        body: JSON.stringify({
          participantId,
          displayName,
          action: 'JOIN'
        })

      })

    }

    client.activate()

    // Announce departure and disconnect when leaving this room
    return () => {

      if (client.connected) {

        client.publish({

          destination: `/app/presence/${roomId}`,

          body: JSON.stringify({
            participantId,
            displayName,
            action: 'LEAVE'
          })

        })

      }

      client.deactivate()
      setParticipants([])

    }

  }, [joined, roomId, participantId, displayName])


  // Create a new room
  async function createRoom() {

    setLoading(true)
    setMessage('Creating room...')

    try {

      const response = await fetch('http://localhost:8080/api/rooms', {
        method: 'POST',
      })

      const data = await response.json()

      if (!response.ok) {
        throw new Error(data.message || 'Could not create room')
      }

      setLanguage('java')
      setCode(starterCode.java)

      // Reset all language drafts for the new room
      setLanguageDrafts({ ...starterCode })

      setVersions([])
      setVersionName('')
      setVersionDescription('')
      setShowVersionForm(false)
      setShowVersionHistory(false)
      setExecutionOutput('')
      setExecutionStatus('')
      setShowPreview(false)
      setParticipants([])

      setRoomId(data.roomId)
      setMessage(data.message)
      setJoined(true)

    } catch (error) {

      setMessage('Error: ' + error.message)

    } finally {

      setLoading(false)

    }

  }


  // Join an existing room
  async function joinRoom() {

    if (!roomId.trim()) {
      setMessage('Please enter a room ID.')
      return
    }

    setLoading(true)
    setMessage('Checking room...')

    try {

      const response = await fetch(
        `http://localhost:8080/api/rooms/${encodeURIComponent(roomId.trim())}`
      )

      const data = await response.json()

      if (!response.ok) {
        throw new Error(data.message || 'Room not found')
      }

      const loadedLanguage = data.language ?? 'java'
      const loadedCode = data.code ?? ''

      setCode(loadedCode)
      setLanguage(loadedLanguage)

      // Initialize drafts with starter code, then load the room's
      // currently saved code into its corresponding language draft
      setLanguageDrafts({
        ...starterCode,
        [loadedLanguage]: loadedCode
      })

      setVersions([])
      setVersionName('')
      setVersionDescription('')
      setShowVersionForm(false)
      setShowVersionHistory(false)
      setExecutionOutput('')
      setExecutionStatus('')
      setShowPreview(false)
      setParticipants([])

      setRoomId(data.roomId)

      setMessage(data.message)
      setJoined(true)

    } catch (error) {

      setMessage('Error: ' + error.message)

    } finally {

      setLoading(false)

    }

  }


  // Save a historical snapshot with optional metadata
  function saveVersion() {

    if (!client.connected) {
      setMessage('WebSocket is not connected. Please try again.')
      return
    }

    client.publish({

      destination: `/app/version/${roomId}`,

      body: JSON.stringify({
        roomId: roomId,
        code: code,
        language: language,
        versionName: versionName.trim(),
        description: versionDescription.trim()
      })

    })

    setMessage('Version save request sent!')

    setVersionName('')
    setVersionDescription('')
    setShowVersionForm(false)

  }


  // Retrieve historical versions and toggle panel visibility
  async function fetchVersions() {

    if (showVersionHistory) {
      setShowVersionHistory(false)
      return
    }

    setShowVersionHistory(true)
    setVersionsLoading(true)

    try {

      const response = await fetch(
        `http://localhost:8080/api/rooms/${encodeURIComponent(roomId)}/versions`
      )

      if (!response.ok) {
        throw new Error('Could not retrieve version history')
      }

      const data = await response.json()

      setVersions(data)
      setMessage('Version history loaded successfully.')

    } catch (error) {

      setMessage('Error: ' + error.message)
      setShowVersionHistory(false)

    } finally {

      setVersionsLoading(false)

    }

  }


  // Delete a saved version
  async function deleteVersion(versionId) {

    const confirmDelete = window.confirm(
      'Are you sure you want to delete this version?'
    )

    if (!confirmDelete) {
      return
    }

    try {

      const response = await fetch(
        `http://localhost:8080/api/rooms/${encodeURIComponent(roomId)}/versions/${versionId}`,
        {
          method: 'DELETE'
        }
      )

      if (!response.ok) {
        throw new Error('Failed to delete version')
      }

      setVersions(previousVersions =>
        previousVersions.filter(
          version => Number(version.version_id) !== Number(versionId)
        )
      )

      setMessage('Version deleted successfully.')

    } catch (error) {

      setMessage('Error: ' + error.message)

    }

  }


  // View a historical snapshot locally
  function viewVersion(version) {

    // Update the corresponding language draft
    setLanguageDrafts(previousDrafts => ({
      ...previousDrafts,
      [version.language]: version.code
    }))

    setCode(version.code)
    setLanguage(version.language)

    setMessage(`Viewing Version #${version.version_number}`)

  }


  // Restore a historical version to the shared room
  function restoreVersion(version) {

    if (!client.connected) {
      setMessage('WebSocket is not connected. Please try again.')
      return
    }

    const confirmed = window.confirm(
      `Are you sure you want to restore Version #${version.version_number}?`
    )

    if (!confirmed) {
      return
    }

    client.publish({

      destination: `/app/restore/${roomId}`,

      body: JSON.stringify({
        roomId: roomId,
        code: version.code,
        language: version.language
      })

    })

    setMessage(
      `Restore request sent for Version #${version.version_id}.`
    )

  }


  // Execute the current code through the Spring Boot Docker API
  async function runCode() {

    const supportedLanguages = ['java', 'c', 'cpp', 'python', 'javascript']

    if (!supportedLanguages.includes(language)) {
      setExecutionStatus('Unsupported')
      setExecutionOutput('Execution is currently available for Java, C, C++, Python and JavaScript.')
      return
    }

    setIsExecuting(true)
    setExecutionStatus('Running')
    setExecutionOutput('Running your code inside Docker...')

    try {

      const response = await fetch('http://localhost:8080/api/execute', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          language: language,
          code: code
        })
      })

      const responseText = await response.text()
      let result

      try {
        result = JSON.parse(responseText)
      } catch {
        result = responseText
      }

      if (!response.ok) {

        const errorMessage = typeof result === 'string'
          ? result
          : result.output || result.message || 'Code execution failed.'

        throw new Error(errorMessage)

      }

      setExecutionOutput(result.output || '(No output)')

      setExecutionStatus(
        result.timedOut
          ? 'Timed out'
          : result.success
            ? 'Completed'
            : 'Failed'
      )

    } catch (error) {

      setExecutionStatus('Error')
      setExecutionOutput(error.message || 'Could not connect to the execution API.')

    } finally {

      setIsExecuting(false)

    }

  }


  // Build a browser preview for HTML or CSS
  function getPreviewDocument() {

    if (language === 'html') {
      return code
    }

    return `<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CodeTogether CSS Preview</title>
    <style>
        ${code}
    </style>
</head>
<body>
    <h1>CSS Live Preview</h1>
    <p>This page is styled using your CSS code.</p>
    <button>Sample Button</button>
    <div class="preview-box">Sample Content</div>
</body>
</html>`

  }


  // Leave the current room
  function leaveRoom() {

    setJoined(false)
    setMessage('')
    setVersions([])
    setVersionName('')
    setVersionDescription('')
    setShowVersionForm(false)
    setShowVersionHistory(false)
    setExecutionOutput('')
    setExecutionStatus('')
    setShowPreview(false)
    setParticipants([])

  }


  // Display editor after joining a room
  if (joined) {

    return (

      <main className="editor-page">

        <header className="editor-header">

          <div className="logo">
            Code<span>Together</span>
          </div>

          <div className="room-info">

            <span>
              Room: <strong>{roomId}</strong>
            </span>

            <button
              className="copy-button"
              onClick={() => navigator.clipboard.writeText(roomId)}
            >
              Copy ID
            </button>

          </div>

          {/* Live collaborator presence */}
          <div className="presence-panel">

            <span className="presence-count">
              <span className="online-dot"></span>
              {participants.length} online
            </span>

            <div className="presence-list">

              {participants.map((participant) => (

                <span
                  className="participant-chip"
                  key={participant.participantId}
                  title={participant.displayName}
                >
                  {participant.displayName}
                </span>

              ))}

            </div>

          </div>

          <button
            className="leave-button"
            onClick={leaveRoom}
          >
            Leave room
          </button>

        </header>


        <section className="workspace">

          <div className="toolbar">

            <div className="toolbar-heading">
              <h2>Code Editor</h2>
              <p>Write and edit your code here.</p>
            </div>


            <label className="language-select">

              Language

              <select
                value={language}
                onChange={(event) => {

                  const newLanguage = event.target.value

                  // Preserve the code in the current language
                  const updatedDrafts = {
                    ...languageDrafts,
                    [language]: code
                  }

                  // Load the previously saved draft for the selected
                  // language, or its starter code if no draft exists
                  const newCode =
                    updatedDrafts[newLanguage] ?? starterCode[newLanguage]

                  // Update both language drafts
                  setLanguageDrafts({
                    ...updatedDrafts,
                    [newLanguage]: newCode
                  })

                  setLanguage(newLanguage)
                  setCode(newCode)

                  setExecutionOutput('')
                  setExecutionStatus('')
                  setShowPreview(false)

                  // Share the language switch with room participants
                  if (client.connected) {

                    client.publish({

                      destination: `/app/code/${roomId}`,

                      body: JSON.stringify({
                        roomId: roomId,
                        code: newCode,
                        language: newLanguage
                      })

                    })

                  }

                }}
              >

                <option value="java">Java</option>
                <option value="c">C</option>
                <option value="cpp">C++</option>
                <option value="python">Python</option>
                <option value="javascript">JavaScript</option>
                <option value="html">HTML</option>
                <option value="css">CSS</option>

              </select>

            </label>


            <button
              className="run-button"
              onClick={runCode}
              disabled={isExecuting || !['java', 'c', 'cpp', 'python', 'javascript'].includes(language)}
              title={
                ['html', 'css'].includes(language)
                  ? 'HTML and CSS preview is available with the Preview button'
                  : 'Run code in Docker'
              }
            >
              {isExecuting ? 'Running...' : '▶ Run'}
            </button>


            {['html', 'css'].includes(language) && (

              <button
                className="preview-button"
                onClick={() => setShowPreview(true)}
              >
                ▶ Preview
              </button>

            )}


            <button
              className="save-version-button"
              onClick={saveVersion}
              disabled={!client.connected}
            >
              Save Version
            </button>


            <button
              className="save-version-button"
              onClick={() => setShowVersionForm(true)}
              disabled={!client.connected}
            >
              Add Version Details
            </button>


            {/* Save Version Form */}
            {showVersionForm && (

              <div className="save-version-form">

                <h3>Save a New Version</h3>

                <label>
                  Version Name (Optional)

                  <input
                    type="text"
                    placeholder="e.g. Initial implementation"
                    value={versionName}
                    onChange={(event) => setVersionName(event.target.value)}
                  />

                </label>

                <label>
                  Description (Optional)

                  <textarea
                    placeholder="Describe what changed..."
                    value={versionDescription}
                    onChange={(event) => setVersionDescription(event.target.value)}
                    rows={3}
                  />

                </label>

                <div className="save-version-form-actions">

                  <button
                    onClick={() => {
                      setShowVersionForm(false)
                      setVersionName('')
                      setVersionDescription('')
                    }}
                  >
                    Cancel
                  </button>

                  <button
                    onClick={saveVersion}
                    disabled={!client.connected}
                  >
                    Save
                  </button>

                </div>

              </div>

            )}


            <button
              className="history-button"
              onClick={fetchVersions}
              disabled={versionsLoading}
            >
              {versionsLoading
                ? 'Loading...'
                : showVersionHistory
                  ? 'Hide Version History'
                  : 'Version History'}
            </button>

          </div>


          {/* Monaco Code Editor */}
          <div className="editor-container">

            <div className="monaco-editor-host">

              <Editor
                height="100%"
                width="100%"
                language={language}
                theme="vs-dark"
                value={code}

                onChange={(value) => {

                  const updatedCode = value || ''

                  setCode(updatedCode)

                  // Update the draft for the current language
                  setLanguageDrafts(previousDrafts => ({
                    ...previousDrafts,
                    [language]: updatedCode
                  }))

                  if (client.connected) {

                    client.publish({

                      destination: `/app/code/${roomId}`,

                      body: JSON.stringify({
                        roomId: roomId,
                        code: updatedCode,
                        language: language
                      })

                    })

                  }

                }}

                options={{
                  minimap: {
                    enabled: false
                  },
                  fontSize: 15,
                  automaticLayout: true,
                  scrollBeyondLastLine: false,
                  scrollBeyondLastColumn: 5,
                  padding: {
                    top: 16,
                    bottom: 16
                  },
                  scrollbar: {
                    vertical: 'auto',
                    horizontal: 'auto',
                    verticalScrollbarSize: 12,
                    horizontalScrollbarSize: 12,
                    alwaysConsumeMouseWheel: false
                  },
                  overviewRulerLanes: 0,
                  wordWrap: 'off'
                }}

              />

            </div>

          </div>


          {/* Code Execution Console */}
          <section className="execution-console">

            <div className="execution-console-header">
              <h3>Execution Console</h3>

              {executionStatus && (
                <span className={`execution-status ${executionStatus.toLowerCase().replaceAll(' ', '-')}`}>
                  {executionStatus}
                </span>
              )}

            </div>

            <pre className="execution-output">
              {executionOutput || 'Click Run to execute your code. Output and errors will appear here.'}
            </pre>

          </section>


          {/* HTML and CSS Browser Preview */}
          {showPreview && ['html', 'css'].includes(language) && (

            <section className="preview-container">

              <div className="preview-header">
                <h3>Browser Preview</h3>

                <button onClick={() => setShowPreview(false)}>
                  Close Preview
                </button>
              </div>

              <iframe
                title="CodeTogether Preview"
                className="preview-frame"
                srcDoc={getPreviewDocument()}
                sandbox=""
              />

            </section>

          )}


          {/* Version History Panel */}
          {showVersionHistory && (

            <div className="version-history">

              <h3>Version History</h3>

              {versionsLoading && (
                <p>Loading versions...</p>
              )}

              {!versionsLoading && versions.length === 0 && (
                <p>No saved versions found.</p>
              )}

              {versions.map((version) => (

                <div
                  className="version-item"
                  key={version.version_id}
                >

                  <div className="version-details">

                    <strong>
                      {version.version_name?.trim()
                        ? version.version_name
                        : `Version #${version.version_id}`}
                    </strong>

                    <span>
                      Version #{version.version_number} | {version.language.toUpperCase()}
                    </span>

                    {version.description && (
                      <p>{version.description}</p>
                    )}

                    <small>
                      {new Date(version.created_at).toLocaleString()}
                    </small>

                  </div>


                  <div className="version-actions">

                    <button
                      className="view-version-button"
                      onClick={() => viewVersion(version)}
                    >
                      View Version
                    </button>

                    <button
                      className="restore-version-button"
                      onClick={() => restoreVersion(version)}
                    >
                      Restore Version
                    </button>

                    <button
                      className="delete-version-button"
                      onClick={() => deleteVersion(version.version_id)}
                    >
                      Delete
                    </button>

                  </div>

                </div>

              ))}

            </div>

          )}


          {/* Editor Footer */}
          <div className="editor-footer">

            <span>Room ID: {roomId}</span>
            <span>{language.toUpperCase()}</span>
            <span>WebSocket sharing enabled</span>

          </div>


          <p className="editor-note">
            Code changes are shared with users connected to this room.
            The latest code and language are saved in the database.
          </p>

          {message && (
            <p className="editor-note" role="status">
              {message}
            </p>
          )}

        </section>

      </main>

    )

  }


  // Display home page before joining a room
  return (

    <main className="app">

      <header className="header">

        <div className="logo">
          Code<span>Together</span>
        </div>

        <span className="status">
          <span className="status-dot"></span>
          Collaboration workspace
        </span>

      </header>


      <section className="hero">

        <p className="eyebrow">
          REAL-TIME COLLABORATION
        </p>

        <h1>
          Build something<br />
          <span>together.</span>
        </h1>

        <p className="subtitle">
          Create a room, invite a friend, and get ready to code together.
        </p>


        <div className="room-card">

          <h2>Start a coding session</h2>

          <p>
            Create a new room or join one using its room ID.
          </p>


          <button
            className="create-button"
            onClick={createRoom}
            disabled={loading}
          >
            {loading ? 'Please wait...' : '+ Create a new room'}
          </button>


          <div className="separator">
            <span>OR JOIN AN EXISTING ROOM</span>
          </div>


          <label htmlFor="roomId">
            Room ID
          </label>

          <input
            id="roomId"
            type="text"
            placeholder="Enter your room ID"
            value={roomId}
            onChange={(event) => setRoomId(event.target.value)}
          />


          <button
            className="join-button"
            onClick={joinRoom}
            disabled={loading}
          >
            {loading ? 'Please wait...' : 'Join room →'}
          </button>


          {message && (

            <div className="message" role="status">
              {message}
            </div>

          )}

        </div>

      </section>


      <footer>
        Built with React and Spring Boot
      </footer>

    </main>

  )

}

export default App