import { useState, useEffect } from 'react';
import {
  KanbanSquare, Plus, Users, CalendarDays, ArrowLeft, Pencil, GripVertical,
  CheckCircle2, CircleDot, Eye, Clock, AlertTriangle,
} from 'lucide-react';
import {
  DndContext, closestCorners, PointerSensor, useSensor, useSensors,
  DragOverlay,
} from '@dnd-kit/core';
import {
  SortableContext, useSortable, verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import PageWrapper from '../../components/layout/PageWrapper';
import {
  Button, Card, Badge, Avatar, Input, Spinner, Modal, Select, useToast,
} from '../../components/ui';
import {
  getProjects, createProject, getKanban, createTask,
  updateTaskStatus, assignTask, getMembers, addMember, updateProject,
} from '../../services/projectService';
import styles from './Projects.module.css';

const STATUSES = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'];
const STATUS_LABELS = { TODO: 'To Do', IN_PROGRESS: 'In Progress', REVIEW: 'Review', DONE: 'Done' };
const STATUS_ICONS = { TODO: CircleDot, IN_PROGRESS: Clock, REVIEW: Eye, DONE: CheckCircle2 };
const PRIORITY_OPTIONS = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
  { value: 'URGENT', label: 'Urgent' },
];

export default function Projects() {
  const toast = useToast();
  const [projects, setProjects] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [board, setBoard] = useState(null);
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newProjectName, setNewProjectName] = useState('');
  const [newTaskTitle, setNewTaskTitle] = useState('');
  const [taskModal, setTaskModal] = useState(null);
  const [activeId, setActiveId] = useState(null);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } })
  );

  useEffect(() => { loadProjects(); }, []);

  useEffect(() => {
    if (selectedId) {
      loadBoard(selectedId);
      loadMembers(selectedId);
    }
  }, [selectedId]);

  const loadProjects = async () => {
    try {
      const data = await getProjects();
      setProjects(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error('Failed to load projects', e);
      setProjects(DEMO_PROJECTS);
    } finally {
      setLoading(false);
    }
  };

  const loadBoard = async (id) => {
    try {
      const data = await getKanban(id);
      setBoard(data);
    } catch (e) {
      console.error('Failed to load board', e);
      setBoard(DEMO_BOARD);
    }
  };

  const loadMembers = async (id) => {
    try {
      const data = await getMembers(id);
      setMembers(Array.isArray(data) ? data : []);
    } catch { setMembers([]); }
  };

  const handleCreateProject = async () => {
    if (!newProjectName.trim()) return;
    try {
      const project = await createProject({ name: newProjectName.trim() });
      setNewProjectName('');
      toast.success('Project created');
      await loadProjects();
      setSelectedId(project.id);
    } catch (e) {
      toast.error(e.message || 'Failed to create project');
    }
  };

  const handleCreateTask = async () => {
    if (!newTaskTitle.trim() || !selectedId) return;
    try {
      await createTask(selectedId, { title: newTaskTitle.trim() });
      setNewTaskTitle('');
      toast.success('Task added');
      await loadBoard(selectedId);
    } catch (e) {
      toast.error(e.message || 'Failed to create task');
    }
  };

  const handleStatusChange = async (taskId, newStatus) => {
    try {
      await updateTaskStatus(taskId, newStatus);
      await loadBoard(selectedId);
      toast.success(`Task moved to ${STATUS_LABELS[newStatus]}`);
    } catch (e) {
      toast.error(e.message || 'Failed to update status');
    }
  };

  const handleAssign = async (taskId, assigneeId) => {
    try {
      await assignTask(taskId, assigneeId);
      await loadBoard(selectedId);
      toast.success('Task assigned');
    } catch (e) {
      toast.error(e.message || 'Failed to assign');
    }
  };

  // DnD handlers
  const findTaskAndColumn = (id) => {
    if (!board) return { task: null, column: null };
    for (const status of STATUSES) {
      const task = (board[status] || []).find((t) => t.id === id);
      if (task) return { task, column: status };
    }
    return { task: null, column: null };
  };

  const handleDragStart = (event) => {
    setActiveId(event.active.id);
  };

  const handleDragEnd = (event) => {
    setActiveId(null);
    const { active, over } = event;
    if (!over || !active) return;

    const { column: fromCol } = findTaskAndColumn(active.id);
    // `over` could be a task id or a column droppable id
    let toCol = STATUSES.find((s) => s === over.id);
    if (!toCol) {
      const overResult = findTaskAndColumn(over.id);
      toCol = overResult.column;
    }

    if (fromCol && toCol && fromCol !== toCol) {
      handleStatusChange(active.id, toCol);
    }
  };

  if (loading) return <PageWrapper title="Projects"><Spinner center /></PageWrapper>;

  const selectedProject = projects.find((p) => p.id === selectedId);

  // ---- Kanban Board View ----
  if (selectedProject && board) {
    const draggedTask = activeId ? findTaskAndColumn(activeId).task : null;

    return (
      <PageWrapper
        title={selectedProject.name}
        subtitle={selectedProject.description || 'Kanban task board'}
        actions={
          <div style={{ display: 'flex', gap: 'var(--space-sm)' }}>
            <Button variant="ghost" onClick={() => { setSelectedId(null); setBoard(null); }} leftIcon={<ArrowLeft size={16} />}>
              All projects
            </Button>
          </div>
        }
      >
        <div className={styles.createForm}>
          <Input
            placeholder="Quick-add a task…"
            value={newTaskTitle}
            onChange={(e) => setNewTaskTitle(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleCreateTask()}
          />
          <Button onClick={handleCreateTask} size="sm" leftIcon={<Plus size={14} />}>
            Add task
          </Button>
        </div>

        {/* Team members bar */}
        {members.length > 0 && (
          <div className={styles.teamBar}>
            <Users size={14} />
            <span className={styles.teamLabel}>Team</span>
            <div className={styles.teamAvatars}>
              {members.map((m) => (
                <Avatar key={m.id || m.userId} name={m.username || m.displayName || '?'} size="xs" />
              ))}
            </div>
            <Badge size="sm" variant="outline">{members.length} members</Badge>
          </div>
        )}

        <DndContext
          sensors={sensors}
          collisionDetection={closestCorners}
          onDragStart={handleDragStart}
          onDragEnd={handleDragEnd}
        >
          <div className={styles.board}>
            {STATUSES.map((status) => {
              const tasks = board[status] || [];
              const StatusIcon = STATUS_ICONS[status];
              return (
                <KanbanColumn key={status} id={status} status={status} label={STATUS_LABELS[status]} icon={StatusIcon} count={tasks.length}>
                  <SortableContext items={tasks.map((t) => t.id)} strategy={verticalListSortingStrategy}>
                    {tasks.map((task) => (
                      <SortableTaskCard
                        key={task.id}
                        task={task}
                        onClick={() => setTaskModal(task)}
                      />
                    ))}
                  </SortableContext>
                </KanbanColumn>
              );
            })}
          </div>
          <DragOverlay>
            {draggedTask ? <TaskCardContent task={draggedTask} isDragging /> : null}
          </DragOverlay>
        </DndContext>

        {/* Task Detail Modal */}
        {taskModal && (
          <TaskDetailModal
            task={taskModal}
            members={members}
            onClose={() => setTaskModal(null)}
            onStatusChange={(status) => { handleStatusChange(taskModal.id, status); setTaskModal(null); }}
            onAssign={(assigneeId) => { handleAssign(taskModal.id, assigneeId); setTaskModal(null); }}
          />
        )}
      </PageWrapper>
    );
  }

  // ---- Project List View ----
  return (
    <PageWrapper
      title="Projects"
      subtitle="Manage collaborative projects and task boards."
      actions={
        <div className={styles.createForm}>
          <Input
            placeholder="New project name…"
            value={newProjectName}
            onChange={(e) => setNewProjectName(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleCreateProject()}
          />
          <Button onClick={handleCreateProject} size="sm" leftIcon={<Plus size={14} />}>
            Create
          </Button>
        </div>
      }
    >
      {projects.length > 0 ? (
        <div className={styles.projectList}>
          {projects.map((p) => (
            <Card key={p.id} className={styles.projectCard} onClick={() => setSelectedId(p.id)}>
              <div className={styles.projectName}>{p.name}</div>
              {p.description && <div className={styles.projectDesc}>{p.description}</div>}
              <div style={{ display: 'flex', gap: 'var(--space-xs)', marginBottom: 'var(--space-sm)' }}>
                <Badge size="sm" variant={p.status === 'ACTIVE' ? 'success' : 'outline'}>{p.status}</Badge>
                <Badge size="sm" variant="outline">{p.visibility}</Badge>
              </div>
              <div className={styles.projectStats}>
                <span className={styles.projectStat}><Users size={12} /> {p.memberCount || 1} members</span>
                <span className={styles.projectStat}><KanbanSquare size={12} /> {p.taskCount || 0} tasks</span>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <div className={styles.empty}>
          <span className={styles.emptyIcon}><KanbanSquare size={28} /></span>
          <h3>No projects yet</h3>
          <p style={{ color: 'var(--color-text-muted)' }}>Create your first project to start collaborating.</p>
        </div>
      )}
    </PageWrapper>
  );
}

/* Kanban Column (droppable) */
function KanbanColumn({ id, status, label, icon: Icon, count, children }) {
  return (
    <div className={styles.column} id={id}>
      <div className={styles.columnHeader}>
        <Icon size={14} className={styles.columnIcon} />
        <span className={styles.columnTitle}>{label}</span>
        <span className={styles.columnCount}>{count}</span>
      </div>
      <div className={styles.columnBody}>
        {children}
      </div>
    </div>
  );
}

/* Sortable task card wrapper */
function SortableTaskCard({ task, onClick }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: task.id,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.4 : 1,
  };

  return (
    <div ref={setNodeRef} style={style} {...attributes} {...listeners}>
      <TaskCardContent task={task} onClick={onClick} />
    </div>
  );
}

/* Task card content (also used in DragOverlay) */
function TaskCardContent({ task, onClick, isDragging }) {
  return (
    <Card className={`${styles.taskCard} ${isDragging ? styles.taskDragging : ''}`} onClick={onClick}>
      <div className={styles.taskTitle}>{task.title}</div>
      {task.description && <div className={styles.taskDesc}>{task.description}</div>}
      <div className={styles.taskFooter}>
        <div className={styles.taskMeta}>
          <span className={`${styles.priorityDot} ${styles['priority' + (task.priority || 'MEDIUM')]}`} />
          <span>{task.priority || 'MEDIUM'}</span>
        </div>
        {task.assigneeUsername && (
          <Avatar name={task.assigneeUsername} size="xs" />
        )}
        {task.dueDate && (
          <span className={styles.taskMeta}>
            <CalendarDays size={12} /> {task.dueDate}
          </span>
        )}
      </div>
    </Card>
  );
}

/* Task Detail Modal */
function TaskDetailModal({ task, members, onClose, onStatusChange, onAssign }) {
  return (
    <Modal open onClose={onClose} title={task.title} size="md">
      <div className={styles.modalContent}>
        {task.description && (
          <div className={styles.modalField}>
            <label className={styles.modalLabel}>Description</label>
            <p className={styles.modalValue}>{task.description}</p>
          </div>
        )}
        <div className={styles.modalRow}>
          <div className={styles.modalField}>
            <label className={styles.modalLabel}>Status</label>
            <Select
              value={task.status || 'TODO'}
              onChange={(e) => onStatusChange(e.target.value)}
              options={STATUSES.map((s) => ({ value: s, label: STATUS_LABELS[s] }))}
            />
          </div>
          <div className={styles.modalField}>
            <label className={styles.modalLabel}>Priority</label>
            <Badge variant={task.priority === 'URGENT' ? 'danger' : task.priority === 'HIGH' ? 'warning' : 'outline'}>
              {task.priority || 'MEDIUM'}
            </Badge>
          </div>
        </div>
        <div className={styles.modalRow}>
          <div className={styles.modalField}>
            <label className={styles.modalLabel}>Assignee</label>
            {members.length > 0 ? (
              <Select
                value={task.assigneeId || ''}
                onChange={(e) => onAssign(e.target.value)}
                options={[
                  { value: '', label: 'Unassigned' },
                  ...members.map((m) => ({ value: m.userId || m.id, label: m.username || m.displayName })),
                ]}
              />
            ) : (
              <span className={styles.modalValue}>{task.assigneeUsername || 'Unassigned'}</span>
            )}
          </div>
          {task.dueDate && (
            <div className={styles.modalField}>
              <label className={styles.modalLabel}>Due Date</label>
              <span className={styles.modalValue}>{task.dueDate}</span>
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
}

const DEMO_PROJECTS = [
  { id: 'p1', name: 'SkillCircle', description: 'AI-driven developer matching platform', status: 'ACTIVE', visibility: 'PUBLIC', memberCount: 3, taskCount: 8 },
  { id: 'p2', name: 'CodeReview Bot', description: 'Automated PR review with GPT-4', status: 'PLANNING', visibility: 'PRIVATE', memberCount: 2, taskCount: 4 },
];

const DEMO_BOARD = {
  TODO: [
    { id: 't1', title: 'Add profile image upload', priority: 'MEDIUM', assigneeUsername: 'alice' },
    { id: 't2', title: 'Write API documentation', priority: 'LOW' },
  ],
  IN_PROGRESS: [
    { id: 't3', title: 'Implement match pipeline UI', priority: 'HIGH', assigneeUsername: 'bob', dueDate: '2026-09-10' },
  ],
  REVIEW: [
    { id: 't4', title: 'Community chat WebSocket', priority: 'HIGH', assigneeUsername: 'alice' },
  ],
  DONE: [
    { id: 't5', title: 'Set up project structure', priority: 'MEDIUM' },
    { id: 't6', title: 'JWT authentication flow', priority: 'URGENT', assigneeUsername: 'bob' },
  ],
};
