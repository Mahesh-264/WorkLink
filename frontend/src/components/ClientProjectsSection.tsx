import { type FormEvent, useEffect, useState } from 'react';
import axios from 'axios';
import api from '../lib/api';
import ClientProjectInsights from './ClientProjectInsights';
import ClientProjectProposals from './ClientProjectProposals';
import ClientActiveProject from './ClientActiveProject';

type Catalog = { id: number; name: string };
type ProjectSkill = { skillId: number; name: string; importance: string };
type Project = { id: number; title: string; description: string; deliverables: string | null; status: string; categoryId: number; category: string; requiredExperience: number; budgetMin: number; budgetMax: number; deadline: string; complexity: string; acceptedFreelancerName?: string | null; skills: ProjectSkill[] };
type DraftSkill = { skillId: number; importance: string };
type ProjectForm = { title: string; description: string; categoryId: string; requiredExperience: string; budgetMin: string; budgetMax: string; deadline: string; complexity: string; deliverables: string; skills: DraftSkill[] };
const blank: ProjectForm = { title: '', description: '', categoryId: '', requiredExperience: '0', budgetMin: '', budgetMax: '', deadline: '', complexity: 'MEDIUM', deliverables: '', skills: [] };
const complexities = ['LOW', 'EASY', 'MEDIUM', 'HIGH', 'HARD', 'EXPERT'];
const importanceOptions = ['REQUIRED', 'IMPORTANT', 'PREFERRED'];

export default function ClientProjectsSection() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [categories, setCategories] = useState<Catalog[]>([]);
  const [skills, setSkills] = useState<Catalog[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState<string | null>(null);
  const [form, setForm] = useState<ProjectForm>(blank);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const loadCatalog = async () => {
    setCatalogLoading(true);
    setCatalogError(null);
    try {
      const [categoryResult, skillResult] = await Promise.all([
        api.get<Catalog[]>('/categories'),
        api.get<Catalog[]>('/skills'),
      ]);
      setCategories(categoryResult.data);
      setSkills(skillResult.data);
    } catch (catError: unknown) {
      setCatalogError(messageFrom(catError, 'Could not load project categories and skills catalog.'));
    } finally {
      setCatalogLoading(false);
    }
  };

  const loadProjects = async (showLoading = true) => {
    if (showLoading) setLoading(true);
    setError(null);
    try {
      const projectResult = await api.get<Project[]>('/client/projects');
      setProjects(projectResult.data);
    } catch (requestError: unknown) {
      if (axios.isAxiosError(requestError) && requestError.response?.status === 404) {
        setProjects([]);
      } else {
        setError(messageFrom(requestError, 'We could not load your projects.'));
      }
    } finally { setLoading(false); }
  };

  const load = async (showLoading = true) => {
    await Promise.allSettled([loadProjects(showLoading), loadCatalog()]);
  };

  useEffect(() => {
    queueMicrotask(() => { void load(); });
  }, []);

  const openNewForm = () => {
    setError(null);
    setNotice(null);
    setEditingId(null);
    setForm(blank);
    setShowForm(true);
  };

  const reset = () => {
    setForm(blank);
    setEditingId(null);
    setShowForm(false);
  };

  const beginEdit = (project: Project) => {
    setError(null);
    setNotice(null);
    setEditingId(project.id);
    setShowForm(true);
    setForm({
      title: project.title,
      description: project.description,
      categoryId: String(project.categoryId),
      requiredExperience: String(project.requiredExperience),
      budgetMin: String(project.budgetMin),
      budgetMax: String(project.budgetMax),
      deadline: project.deadline,
      complexity: project.complexity,
      deliverables: project.deliverables ?? '',
      skills: project.skills.map(({ skillId, importance }) => ({ skillId, importance })),
    });
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setNotice(null);
    if (!form.skills.length) {
      setError('Add at least one required or preferred skill before saving.');
      return;
    }
    setSubmitting(true);
    try {
      const payload = {
        ...form,
        categoryId: Number(form.categoryId),
        requiredExperience: Number(form.requiredExperience),
        budgetMin: Number(form.budgetMin),
        budgetMax: Number(form.budgetMax),
      };
      if (editingId) await api.put(`/client/projects/${editingId}`, payload);
      else await api.post('/client/projects', payload);
      setNotice(editingId ? 'Draft project updated.' : 'Draft project created.');
      reset();
      await loadProjects(false);
    } catch (requestError) {
      setError(messageFrom(requestError, 'We could not save this project.'));
    } finally {
      setSubmitting(false);
    }
  };

  const publish = async (project: Project) => {
    setError(null);
    setNotice(null);
    try {
      await api.patch(`/client/projects/${project.id}/publish`);
      setNotice(`“${project.title}” is now published.`);
      await loadProjects(false);
    } catch (requestError) {
      setError(messageFrom(requestError, 'We could not publish this project.'));
    }
  };

  const remove = async (project: Project) => {
    if (!window.confirm(`Delete draft “${project.title}”?`)) return;
    setError(null);
    setNotice(null);
    try {
      await api.delete(`/client/projects/${project.id}`);
      setNotice('Project deleted.');
      await loadProjects(false);
    } catch (requestError) {
      setError(messageFrom(requestError, 'We could not delete this project.'));
    }
  };

  const setSkill = (skillId: number, importance: string) =>
    setForm((current) => ({
      ...current,
      skills: current.skills.map((skill) =>
        skill.skillId === skillId ? { ...skill, importance } : skill
      ),
    }));

  return (
    <section className="mt-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-xl font-bold">Projects</h2>
          <p className="mt-1 text-sm text-slate-500">Create draft briefs, refine requirements, and publish opportunities.</p>
        </div>
        <button
          type="button"
          onClick={openNewForm}
          className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
        >
          New project
        </button>
      </div>

      {error && <div><Message kind="error" text={error} /><button type="button" onClick={() => void load()} className="mt-2 text-sm font-semibold text-indigo-700 hover:underline">Retry loading projects</button></div>}
      {notice && <Message kind="success" text={notice} />}

      {showForm && (
        <form onSubmit={submit} className="mt-6 grid gap-4 rounded-xl border border-indigo-100 bg-indigo-50 p-5 sm:grid-cols-2">
          <div className="sm:col-span-2 flex items-center justify-between">
            <h3 className="font-semibold">{editingId ? 'Edit draft project' : 'New draft project'}</h3>
            <button type="button" onClick={reset} className="text-sm text-indigo-700 hover:underline">Cancel</button>
          </div>

          {catalogError && (
            <div className="sm:col-span-2 rounded-lg bg-amber-50 p-3 text-xs text-amber-800 flex items-center justify-between">
              <span>{catalogError}</span>
              <button
                type="button"
                onClick={() => void loadCatalog()}
                className="font-semibold underline ml-2 hover:text-amber-900"
              >
                Retry
              </button>
            </div>
          )}

          <Input
            required
            id="project-title"
            name="title"
            label="Project title"
            value={form.title}
            change={(title) => setForm({ ...form, title })}
          />

          <Select
            required
            id="project-category"
            name="categoryId"
            label="Category"
            value={form.categoryId}
            change={(categoryId) => setForm({ ...form, categoryId })}
            options={categories.map((item) => ({ value: String(item.id), label: item.name }))}
            empty={catalogLoading ? 'Loading categories…' : catalogError ? 'Unable to load categories' : 'Choose a category'}
            disabled={catalogLoading || categories.length === 0}
          />

          <Input
            required
            id="project-description"
            name="description"
            label="Description"
            textarea
            value={form.description}
            change={(description) => setForm({ ...form, description })}
          />

          <Input
            id="project-deliverables"
            name="deliverables"
            label="Deliverables"
            textarea
            value={form.deliverables}
            change={(deliverables) => setForm({ ...form, deliverables })}
          />

          <Input
            required
            id="project-required-experience"
            name="requiredExperience"
            label="Experience required (years)"
            type="number"
            min="0"
            value={form.requiredExperience}
            change={(requiredExperience) => setForm({ ...form, requiredExperience })}
          />

          <Select
            id="project-complexity"
            name="complexity"
            label="Complexity"
            value={form.complexity}
            change={(complexity) => setForm({ ...form, complexity })}
            options={complexities.map((value) => ({ value, label: humanize(value) }))}
          />

          <Input
            required
            id="project-budget-min"
            name="budgetMin"
            label="Minimum budget"
            type="number"
            min="0"
            step="0.01"
            value={form.budgetMin}
            change={(budgetMin) => setForm({ ...form, budgetMin })}
          />

          <Input
            required
            id="project-budget-max"
            name="budgetMax"
            label="Maximum budget"
            type="number"
            min="0"
            step="0.01"
            value={form.budgetMax}
            change={(budgetMax) => setForm({ ...form, budgetMax })}
          />

          <Input
            required
            id="project-deadline"
            name="deadline"
            label="Deadline"
            type="date"
            value={form.deadline}
            change={(deadline) => setForm({ ...form, deadline })}
          />

          <Select
            id="project-add-skill"
            name="addSkill"
            label="Add a skill"
            value=""
            change={(value) =>
              value && setForm({ ...form, skills: [...form.skills, { skillId: Number(value), importance: 'REQUIRED' }] })
            }
            options={skills
              .filter((skill) => !form.skills.some((current) => current.skillId === skill.id))
              .map((item) => ({ value: String(item.id), label: item.name }))}
            empty={catalogLoading ? 'Loading skills…' : catalogError ? 'Unable to load skills' : 'Choose a skill'}
            disabled={catalogLoading || skills.length === 0}
          />

          <div className="sm:col-span-2 flex flex-wrap gap-2">
            {form.skills.map((skill) => (
              <span key={skill.skillId} className="inline-flex items-center gap-2 rounded-lg bg-white px-2 py-1.5 text-sm shadow-sm">
                <span>{skills.find((item) => item.id === skill.skillId)?.name}</span>
                <select
                  aria-label="Skill importance"
                  value={skill.importance}
                  onChange={(event) => setSkill(skill.skillId, event.target.value)}
                  className="rounded border border-slate-200 px-1 py-0.5 text-xs"
                >
                  {importanceOptions.map((value) => (
                    <option key={value} value={value}>{humanize(value)}</option>
                  ))}
                </select>
                <button
                  type="button"
                  aria-label="Remove skill"
                  onClick={() => setForm({ ...form, skills: form.skills.filter((current) => current.skillId !== skill.skillId) })}
                  className="text-rose-600 hover:text-rose-800"
                >
                  ×
                </button>
              </span>
            ))}
          </div>

          <button
            disabled={submitting}
            className="sm:col-span-2 rounded-lg bg-indigo-600 p-2.5 font-semibold text-white hover:bg-indigo-700 disabled:bg-indigo-400"
          >
            {submitting ? 'Saving…' : 'Save draft'}
          </button>
        </form>
      )}

      {loading ? (
        <p className="mt-6 text-sm text-slate-500">Loading projects…</p>
      ) : projects.length === 0 ? (
        <p className="mt-6 rounded-xl border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500">
          Your workspace is ready for its first project.
        </p>
      ) : (
        <div className="mt-6 grid gap-4">
          {(['DRAFT', 'PUBLISHED', 'IN_PROGRESS'] as const).map((status) => {
            const items = projects.filter((project) => project.status === status);
            if (!items.length) return null;
            return <div key={status} className="space-y-3"><h3 className="text-sm font-bold uppercase tracking-wide text-slate-500">{status === 'IN_PROGRESS' ? 'My active projects' : humanize(status) + ' projects'}</h3>{items.map((project) => (
            <ProjectCard key={project.id} project={project} edit={beginEdit} publish={publish} remove={remove} />
          ))}</div>;
          })}
        </div>
      )}
    </section>
  );
}

function ProjectCard({ project, edit, publish, remove }: { project: Project; edit: (project: Project) => void; publish: (project: Project) => void; remove: (project: Project) => void }) {
  return (
    <article className="rounded-xl border border-slate-200 p-5">
      <div className="flex flex-col justify-between gap-3 sm:flex-row">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="font-bold">{project.title}</h3>
            <Status value={project.status} />
          </div>
          <p className="mt-1 text-sm text-slate-500">
            {project.category} · ${Number(project.budgetMin).toLocaleString()}–${Number(project.budgetMax).toLocaleString()} · Due {new Date(`${project.deadline}T00:00:00`).toLocaleDateString()}
          </p>
        </div>
        {project.status === 'DRAFT' && (
          <div className="flex gap-3 text-sm font-semibold">
            <button onClick={() => edit(project)} className="text-indigo-700 hover:underline">Edit</button>
            <button onClick={() => publish(project)} className="rounded bg-emerald-600 px-3 py-1.5 text-white hover:bg-emerald-700">Publish Project</button>
            <button onClick={() => remove(project)} className="text-rose-700 hover:underline">Delete</button>
          </div>
        )}
      </div>
      <p className="mt-3 text-sm leading-6 text-slate-600">{project.description}</p>
      <div className="mt-4 flex flex-wrap gap-2">
        {project.skills.map((skill) => (
          <span key={skill.skillId} className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
            {skill.name} · {humanize(skill.importance)}
          </span>
        ))}
      </div>
      {project.status === 'PUBLISHED' && (
        <>
            <ClientProjectInsights projectId={project.id} />
            <ClientProjectProposals projectId={project.id} />
        </>
      )}
      {project.status === 'IN_PROGRESS' && (
        <>
            <p className="mt-4 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">Active with {project.acceptedFreelancerName ?? 'the accepted freelancer'}.</p>
            <ClientActiveProject projectId={project.id} />
        </>
      )}
    </article>
  );
}

function Input({ label, id, name, value, change, required, type = 'text', min, step, textarea, autoComplete = 'off' }: { label: string; id?: string; name?: string; value: string; change: (value: string) => void; required?: boolean; type?: string; min?: string; step?: string; textarea?: boolean; autoComplete?: string }) {
  const inputId = id || (name ? `input-${name}` : undefined);
  return (
    <label htmlFor={inputId} className="block text-sm font-medium text-slate-700">
      {label}{required && <span className="text-rose-600"> *</span>}
      {textarea ? (
        <textarea
          id={inputId}
          name={name}
          required={required}
          value={value}
          onChange={(event) => change(event.target.value)}
          rows={3}
          className="mt-1.5 w-full rounded-lg border border-indigo-200 bg-white px-3 py-2"
        />
      ) : (
        <input
          id={inputId}
          name={name}
          autoComplete={autoComplete}
          required={required}
          type={type}
          min={min}
          step={step}
          value={value}
          onChange={(event) => change(event.target.value)}
          className="mt-1.5 w-full rounded-lg border border-indigo-200 bg-white px-3 py-2"
        />
      )}
    </label>
  );
}

function Select({ label, id, name, value, change, options, empty, required, disabled }: { label: string; id?: string; name?: string; value: string; change: (value: string) => void; options: { value: string; label: string }[]; empty?: string; required?: boolean; disabled?: boolean }) {
  const selectId = id || (name ? `select-${name}` : undefined);
  return (
    <label htmlFor={selectId} className="block text-sm font-medium text-slate-700">
      {label}{required && <span className="text-rose-600"> *</span>}
      <select
        id={selectId}
        name={name}
        required={required}
        disabled={disabled}
        value={value}
        onChange={(event) => change(event.target.value)}
        className="mt-1.5 w-full rounded-lg border border-indigo-200 bg-white px-3 py-2 disabled:bg-slate-100 disabled:text-slate-400"
      >
        {empty && <option value="">{empty}</option>}
        {options.map((option) => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
    </label>
  );
}

function Status({ value }: { value: string }) {
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${value === 'PUBLISHED' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>
      {humanize(value)}
    </span>
  );
}

function Message({ kind, text }: { kind: 'error' | 'success'; text: string }) {
  return (
    <p role={kind === 'error' ? 'alert' : 'status'} className={`mt-5 rounded-lg px-3 py-2 text-sm ${kind === 'error' ? 'bg-rose-50 text-rose-700' : 'bg-emerald-50 text-emerald-700'}`}>
      {text}
    </p>
  );
}

function humanize(value: string) {
  return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function messageFrom(error: unknown, fallback: string) {
  return axios.isAxiosError<{ message?: string }>(error) ? error.response?.data?.message ?? fallback : fallback;
}
