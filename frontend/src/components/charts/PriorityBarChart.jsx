import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell
} from 'recharts';

const PRIORITY_CONFIG = {
  HIGH:   { label: 'High',   color: '#FF5C7A', order: 1 },
  MEDIUM: { label: 'Medium', color: '#FACC15', order: 2 },
  LOW:    { label: 'Low',    color: '#4ADE80', order: 3 },
};

const CustomTooltip = ({ active, payload }) => {
  if (!active || !payload?.length) return null;
  const { name, value } = payload[0].payload;
  return (
    <div style={{
      background: 'var(--bg-elevated)',
      border: 'var(--border-glass)',
      borderRadius: 'var(--radius-md)',
      padding: '10px 14px',
      fontSize: '0.82rem',
      boxShadow: 'var(--shadow-md)',
    }}>
      <p style={{ color: 'var(--text-primary)', fontWeight: 600, marginBottom: 2 }}>{name} Priority</p>
      <p style={{ color: 'var(--text-muted)' }}>{value.toLocaleString()} emails</p>
    </div>
  );
};

export default function PriorityBarChart({ countByPriority }) {
  const data = Object.entries(countByPriority || {})
    .map(([key, value]) => {
      const cfg = PRIORITY_CONFIG[key.toUpperCase()] ?? PRIORITY_CONFIG.LOW;
      return { name: cfg.label, value, color: cfg.color, order: cfg.order };
    })
    .sort((a, b) => a.order - b.order);

  if (!data.length || data.every(d => d.value === 0)) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center',
        height: 260, color: 'var(--text-muted)', fontSize: '0.85rem' }}>
        No priority data yet
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={data} barCategoryGap="30%">
        <XAxis
          dataKey="name"
          axisLine={false}
          tickLine={false}
          tick={{ fill: 'var(--text-muted)', fontSize: 12, fontFamily: 'Outfit, sans-serif' }}
        />
        <YAxis
          axisLine={false}
          tickLine={false}
          tick={{ fill: 'var(--text-muted)', fontSize: 11 }}
          allowDecimals={false}
        />
        <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(255,255,255,0.03)' }} />
        <Bar dataKey="value" radius={[8, 8, 0, 0]} animationDuration={800}>
          {data.map((entry, idx) => (
            <Cell key={idx} fill={entry.color} fillOpacity={0.85} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
